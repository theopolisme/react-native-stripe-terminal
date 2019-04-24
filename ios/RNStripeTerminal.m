#import "RNStripeTerminal.h"
#import <React/RCTConvert.h>

#if __has_include(<StripeTerminal/StripeTerminal.h>)
#import <StripeTerminal/StripeTerminal.h>
#else
#import "StripeTerminal.h"
#endif

@implementation RNStripeTerminal

static dispatch_once_t onceToken = 0;

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[
             @"log",
             @"requestConnectionToken",
             @"readersDiscovered",
             @"readerDiscoveryCompletion",
             @"readerDisconnectCompletion",
             @"readerConnection",
             @"paymentCreation",
             @"didBeginWaitingForReaderInput",
             @"didRequestReaderInputPrompt",
             @"didReportReaderEvent",
             @"didChangePaymentStatus",
             @"didChangeConnectionStatus",
             @"didDisconnectUnexpectedlyFromReader",
             @"connectedReader",
             @"connectionStatus",
             @"paymentStatus",
             @"lastReaderEvent",
             @"abortCreatePaymentCompletion",
             @"abortDiscoverReadersCompletion"
             ];
}

- (NSDictionary *)constantsToExport
{
    return @{
             @"DeviceTypeChipper2X": @(SCPDeviceTypeChipper2X),
             @"DeviceTypeReaderSimulator": @(SCPDeviceTypeReaderSimulator),
             
             @"DiscoveryMethodBluetoothScan": @(SCPDiscoveryMethodBluetoothScan),
             @"DiscoveryMethodBluetoothProximity": @(SCPDiscoveryMethodBluetoothProximity),
             
             @"PaymentIntentStatusRequiresSource": @(SCPPaymentIntentStatusRequiresSource),
             @"PaymentIntentStatusRequiresConfirmation": @(SCPPaymentIntentStatusRequiresConfirmation),
             @"PaymentIntentStatusRequiresCapture": @(SCPPaymentIntentStatusRequiresCapture),
             @"PaymentIntentStatusCanceled": @(SCPPaymentIntentStatusCanceled),
             @"PaymentIntentStatusSucceeded": @(SCPPaymentIntentStatusSucceeded),
             
             @"ReaderEventCardInserted": @(SCPReaderEventCardInserted),
             @"ReaderEventCardRemoved": @(SCPReaderEventCardRemoved),
             
             @"PaymentStatusNotReady": @(SCPPaymentStatusNotReady),
             @"PaymentStatusReady": @(SCPPaymentStatusReady),
             @"PaymentStatusCollectingPaymentMethod": @(SCPPaymentStatusCollectingPaymentMethod),
             @"PaymentStatusConfirmingPaymentIntent": @(SCPPaymentStatusConfirmingPaymentIntent),
             
             @"ConnectionStatusNotConnected": @(SCPConnectionStatusNotConnected),
             @"ConnectionStatusConnected": @(SCPConnectionStatusConnected),
             @"ConnectionStatusBusy": @(SCPConnectionStatusBusy),
             };
}

- (void)fetchConnectionToken:(SCPConnectionTokenCompletionBlock)completion {
    pendingConnectionTokenCompletionBlock = completion;
    [self sendEventWithName:@"requestConnectionToken" body:@{}];
}

- (void)terminal:(SCPTerminal *)terminal didUpdateDiscoveredReaders:(NSArray<SCPReader *>*)_readers {
    readers = _readers;
    
    NSMutableArray *data = [NSMutableArray arrayWithCapacity:[readers count]];
    [readers enumerateObjectsUsingBlock:^(SCPReader *reader, NSUInteger idx, BOOL *stop) {
        [data addObject:[self serializeReader:reader]];
    }];
    
    [self sendEventWithName:@"readersDiscovered" body:data];
}

- (void)onLogEntry:(NSString * _Nonnull) logline {
    if (self.bridge == nil) {
        return;
    }

    [self sendEventWithName:@"log" body:logline];
}

RCT_EXPORT_METHOD(setConnectionToken:(NSString *)token error:(NSString *)errorMessage) {
    if (pendingConnectionTokenCompletionBlock) {
        if ([errorMessage length] != 0) {
            NSError* error = [NSError errorWithDomain:@"com.stripe-terminal.rn" code:1 userInfo:[NSDictionary dictionaryWithObject:errorMessage forKey:NSLocalizedDescriptionKey]];
            pendingConnectionTokenCompletionBlock(nil, error);
        } else {
            pendingConnectionTokenCompletionBlock(token, nil);
        }
        
        pendingConnectionTokenCompletionBlock = nil;
    }
}

RCT_EXPORT_METHOD(initialize) {
    dispatch_once(&onceToken, ^{
        [SCPTerminal setTokenProvider:self];
    });
    
    SCPTerminal.shared.delegate = self;
    [SCPTerminal setLogListener:^(NSString * _Nonnull logline) {
        [self onLogEntry:logline];
    }];
    SCPTerminal.shared.logLevel = SCPLogLevelVerbose;

    // When the React module is initialized, abort any pending calls that may not have been
    // cleaned up from a previous initialization (e.g., due to hot reloading).
    [self abortDiscoverReaders];
    [self abortCreatePayment];

    // When the module is initialized, assume the card has been removed.
    lastReaderEvent = SCPReaderEventCardRemoved;
}

RCT_EXPORT_METHOD(discoverReaders:(NSInteger *)deviceType method:(NSInteger *)method) {
    // Attempt to abort any pending discoverReader calls first.
    [self abortDiscoverReaders];

    SCPDiscoveryConfiguration *config = [[SCPDiscoveryConfiguration alloc] initWithDeviceType:(SCPDeviceType)deviceType method:(SCPDiscoveryMethod)method];
    pendingDiscoverReaders = [SCPTerminal.shared discoverReaders:config delegate:self completion:^(NSError * _Nullable error) {
        pendingDiscoverReaders = nil;
        if (error) {
            [self sendEventWithName:@"readerDiscoveryCompletion" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"readerDiscoveryCompletion" body:@{}];
        }
    }];
}

RCT_EXPORT_METHOD(connectReader:(NSString *)serialNumber ) {
    unsigned long readerIndex = [readers indexOfObjectPassingTest:^(SCPReader *reader, NSUInteger idx, BOOL *stop) {
        return [reader.serialNumber isEqualToString:serialNumber];
    }];
    
    [SCPTerminal.shared connectReader:readers[readerIndex] completion:^(SCPReader * _Nullable reader_, NSError * _Nullable error) {
        reader = reader_;
        if (error) {
            [self sendEventWithName:@"readerConnection" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"readerConnection" body:[self serializeReader:reader]];
        }
    }];
}

- (NSDictionary *)serializeReader:(SCPReader *)reader {
    return @{
             @"batteryLevel": reader.batteryLevel ? reader.batteryLevel : @(0),
             @"deviceType": @(reader.deviceType),
             @"serialNumber": reader.serialNumber ? reader.serialNumber : @"",
             @"deviceSoftwareVersion": reader.deviceSoftwareVersion ? reader.deviceSoftwareVersion : @""
             };
}

- (NSDictionary *)serializePaymentIntent:(SCPPaymentIntent *)intent {
    return @{
             @"stripeId": intent.stripeId,
             @"created": intent.created,
             @"status": @(intent.status),
             @"amount": @(intent.amount),
             @"currency": intent.currency,
             @"metadata": intent.metadata
             };
}

RCT_EXPORT_METHOD(createPayment:(NSDictionary *)options) {
    void (^onIntent) (SCPPaymentIntent * _Nullable intent, NSError * _Nullable error) = ^(SCPPaymentIntent * _Nullable intent, NSError * _Nullable creationError) {
        if (creationError) {
            [self sendEventWithName:@"paymentCreation" body:@{
                                                              @"error": [creationError localizedDescription],
                                                              @"code": @(creationError.code)
                                                              }];
            
        } else {
            pendingCreatePaymentIntent = [SCPTerminal.shared collectPaymentMethod:intent delegate:self completion:^(SCPPaymentIntent * _Nullable collectedIntent, NSError * _Nullable collectionError) {
                pendingCreatePaymentIntent = nil;
                if (collectionError) {
                    [self sendEventWithName:@"paymentCreation" body:@{
                                                                            @"error": [collectionError localizedDescription],
                                                                            @"code": @(collectionError.code),
                                                                            @"intent": [self serializePaymentIntent:intent]
                                                                            }];
                    
                } else {
                    [SCPTerminal.shared confirmPaymentIntent:collectedIntent completion:^(SCPPaymentIntent * _Nullable confirmedIntent, SCPConfirmError * _Nullable confirmationError) {
                        if (confirmationError) {
                            [self sendEventWithName:@"paymentCreation" body:@{
                                                                                    @"error": [confirmationError localizedDescription],
                                                                                    @"code": @(confirmationError.code),
                                                                                    @"intent": [self serializePaymentIntent:collectedIntent]
                                                                                    }];
                            
                        } else {
                            [self sendEventWithName:@"paymentCreation" body:@{@"intent": [self serializePaymentIntent:confirmedIntent]}];
                        }
                    }];
                }
            }];
        }
    };
    
    NSString *paymentIntent = [RCTConvert NSString:options[@"paymentIntent"]];
    
    if (paymentIntent) {
        [SCPTerminal.shared retrievePaymentIntent:paymentIntent completion:onIntent];
        
    } else {
        NSInteger amount = [RCTConvert NSInteger:options[@"amount"]];
        NSString *currency = [RCTConvert NSString:options[@"currency"]];
        
        SCPPaymentIntentParameters *params = [[SCPPaymentIntentParameters alloc] initWithAmount:amount currency:currency];
        
        NSInteger applicationFeeAmount = [RCTConvert NSInteger:options[@"applicationFeeAmount"]];
        if (applicationFeeAmount) {
            params.applicationFeeAmount = [NSNumber numberWithInteger:applicationFeeAmount];
        }

        [SCPTerminal.shared createPaymentIntent:params completion:onIntent];
    }
}

- (void)terminal:(SCPTerminal *)terminal didBeginWaitingForReaderInput:(SCPReaderInputOptions)inputOptions {
    [self sendEventWithName:@"didBeginWaitingForReaderInput" body:
     @{
       @"text": [SCPTerminal stringFromReaderInputOptions:inputOptions]
       }];
}

- (void)terminal:(SCPTerminal *)terminal didRequestReaderInputPrompt:(SCPReaderInputPrompt)inputPrompt {
    [self sendEventWithName:@"didRequestReaderInputPrompt" body:
     @{
       @"text": [SCPTerminal stringFromReaderInputPrompt:inputPrompt]
       }];
}

- (void)terminal:(SCPTerminal *)terminal didReportReaderEvent:(SCPReaderEvent)event info:(NSDictionary *)info {
    lastReaderEvent = event;
    [self sendEventWithName:@"didReportReaderEvent" body:
     @{
       @"event": @(event)
       }];
}

- (void)terminal:(SCPTerminal *)terminal didChangePaymentStatus:(SCPPaymentStatus)status {
    [self sendEventWithName:@"didChangePaymentStatus" body:
     @{
       @"status": @(status)
       }];
}

- (void)terminal:(SCPTerminal *)terminal didChangeConnectionStatus:(SCPConnectionStatus)status {
    [self sendEventWithName:@"didChangeConnectionStatus" body:
     @{
       @"status": @(status)
       }];
}

- (void)terminal:(SCPTerminal *)terminal didDisconnectUnexpectedlyFromReader:(SCPReader *)reader {
    [self sendEventWithName:@"didDisconnectUnexpectedlyFromReader" body:[self serializeReader:reader]];
}

RCT_EXPORT_METHOD(clearCachedCredentials) {
    [SCPTerminal.shared clearCachedCredentials];
}

RCT_EXPORT_METHOD(disconnectReader) {
    if (!SCPTerminal.shared.connectedReader) {
        // No reader connected => "success"
        [self sendEventWithName:@"readerDisconnectCompletion" body:@{}];
        return;
    }

    [SCPTerminal.shared disconnectReader:^(NSError * _Nullable error) {
        if (error) {
            [self sendEventWithName:@"readerDisconnectCompletion" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"readerDisconnectCompletion" body:@{}];
        }
    }];
}

RCT_EXPORT_METHOD(getConnectedReader) {
    SCPReader *reader = SCPTerminal.shared.connectedReader;
    [self sendEventWithName:@"connectedReader" body:
     reader ? [self serializeReader:reader] : @{}];
}

RCT_EXPORT_METHOD(abortCreatePayment) {
    if (pendingCreatePaymentIntent) {
        [pendingCreatePaymentIntent cancel:^(NSError * _Nullable error) {
            if (error) {
                [self sendEventWithName:@"abortCreatePaymentCompletion" body:@{@"error": [error localizedDescription]}];
            } else {
                pendingCreatePaymentIntent = nil;
                [self sendEventWithName:@"abortCreatePaymentCompletion" body:@{}];
            }
        }];
        return;
    }
    
    [self sendEventWithName:@"abortCreatePaymentCompletion" body:@{}];
}

RCT_EXPORT_METHOD(abortDiscoverReaders) {
    if (pendingDiscoverReaders) {
        [pendingDiscoverReaders cancel:^(NSError * _Nullable error) {
            if (error) {
                [self sendEventWithName:@"abortDiscoverReadersCompletion" body:@{@"error": [error localizedDescription]}];
            } else {
                pendingDiscoverReaders = nil;
                [self sendEventWithName:@"abortDiscoverReadersCompletion" body:@{}];
            }
        }];
        return;
    }
    
    [self sendEventWithName:@"abortDiscoverReadersCompletion" body:@{}];
}

RCT_EXPORT_METHOD(getConnectionStatus) {
    SCPConnectionStatus status = SCPTerminal.shared.connectionStatus;
    [self sendEventWithName:@"connectionStatus" body:@(status)];
}

RCT_EXPORT_METHOD(getPaymentStatus) {
    SCPPaymentStatus status = SCPTerminal.shared.paymentStatus;
    [self sendEventWithName:@"paymentStatus" body:@(status)];
}

RCT_EXPORT_METHOD(getLastReaderEvent) {
    [self sendEventWithName:@"lastReaderEvent" body:@(lastReaderEvent)];
}

RCT_EXPORT_MODULE()

@end
