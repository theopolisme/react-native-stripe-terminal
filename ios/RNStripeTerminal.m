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
             @"readerSoftwareUpdateProgress",
             @"readerDiscoveryCompletion",
             @"readerDisconnectCompletion",
             @"readerConnection",
             @"updateCheck",
             @"updateInstall",
             @"paymentCreation",
             @"paymentIntentCreation",
             @"paymentIntentRetrieval",
             @"paymentMethodCollection",
             @"paymentProcess",
             @"paymentIntentCancel",
             @"didBeginWaitingForReaderInput",
             @"didRequestReaderInput",
             @"didRequestReaderDisplayMessage",
             @"didReportReaderEvent",
             @"didReportUnexpectedReaderDisconnect",
             @"didReportLowBatteryWarning",
             @"didChangePaymentStatus",
             @"didChangeConnectionStatus",
             @"didDisconnectUnexpectedlyFromReader",
             @"connectedReader",
             @"connectionStatus",
             @"paymentStatus",
             @"lastReaderEvent",
             @"abortCreatePaymentCompletion",
             @"abortDiscoverReadersCompletion",
             @"abortInstallUpdateCompletion"
             ];
}

- (NSDictionary *)constantsToExport
{
    return @{
             @"DeviceTypeChipper2X": @(SCPDeviceTypeChipper2X),

             @"DiscoveryMethodBluetoothScan": @(SCPDiscoveryMethodBluetoothScan),
             @"DiscoveryMethodBluetoothProximity": @(SCPDiscoveryMethodBluetoothProximity),

             @"PaymentIntentStatusRequiresPaymentMethod": @(SCPPaymentIntentStatusRequiresPaymentMethod),
             @"PaymentIntentStatusRequiresConfirmation": @(SCPPaymentIntentStatusRequiresConfirmation),
             @"PaymentIntentStatusRequiresCapture": @(SCPPaymentIntentStatusRequiresCapture),
             @"PaymentIntentStatusCanceled": @(SCPPaymentIntentStatusCanceled),
             @"PaymentIntentStatusSucceeded": @(SCPPaymentIntentStatusSucceeded),

             @"ReaderEventCardInserted": @(SCPReaderEventCardInserted),
             @"ReaderEventCardRemoved": @(SCPReaderEventCardRemoved),

             @"PaymentStatusNotReady": @(SCPPaymentStatusNotReady),
             @"PaymentStatusReady": @(SCPPaymentStatusReady),
             @"PaymentStatusWaitingForInput": @(SCPPaymentStatusWaitingForInput),
             @"PaymentStatusProcessing": @(SCPPaymentStatusProcessing),

             @"ConnectionStatusNotConnected": @(SCPConnectionStatusNotConnected),
             @"ConnectionStatusConnected": @(SCPConnectionStatusConnected),
             @"ConnectionStatusConnecting": @(SCPConnectionStatusConnecting),
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

- (void)terminal:(SCPTerminal *)terminal didReportReaderSoftwareUpdateProgress:(float)progress {
    [self sendEventWithName:@"readerSoftwareUpdateProgress" body:[NSNumber numberWithFloat:progress]];
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
    [self abortInstallUpdate];

    // When the module is initialized, assume the card has been removed.
    lastReaderEvent = SCPReaderEventCardRemoved;
}

RCT_EXPORT_METHOD(discoverReaders:(NSInteger *)deviceType method:(NSInteger *)method simulated:(BOOL *)simulated) {
    // Attempt to abort any pending discoverReader calls first.
    [self abortDiscoverReaders];

    SCPDiscoveryConfiguration *config = [[SCPDiscoveryConfiguration alloc] initWithDeviceType:(SCPDeviceType)deviceType
                                                                              discoveryMethod:(SCPDiscoveryMethod)method
                                                                                    simulated:simulated];
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

RCT_EXPORT_METHOD(checkForUpdate) {
    [SCPTerminal.shared checkForUpdate:^(SCPReaderSoftwareUpdate * _Nullable update_, NSError * _Nullable error) {
        update = update_;
        if (error) {
            [self sendEventWithName:@"updateCheck" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"updateCheck" body:[self serializeUpdate:update]];
        }
    }];
}

RCT_EXPORT_METHOD(installUpdate) {
    pendingInstallUpdate = [SCPTerminal.shared installUpdate:update delegate:self completion:^(NSError * _Nullable error) {
        if (error) {
            [self sendEventWithName:@"updateInstall" body:@{@"error": [error localizedDescription]}];
        } else {
            update = nil;
            [self sendEventWithName:@"updateInstall" body:@{}];
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

- (NSDictionary *)serializeUpdate:(SCPReaderSoftwareUpdate *)update {
    return @{
             @"estimatedUpdateTime": [SCPReaderSoftwareUpdate stringFromUpdateTimeEstimate:update.estimatedUpdateTime],
             @"deviceSoftwareVersion": update.deviceSoftwareVersion ? update.deviceSoftwareVersion : @""
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
                    [SCPTerminal.shared processPayment:collectedIntent completion:^(SCPPaymentIntent * _Nullable confirmedIntent, SCPProcessPaymentError * _Nullable processError) {
                        if (processError) {
                            [self sendEventWithName:@"paymentCreation" body:@{
                                                                                    @"error": [processError localizedDescription],
                                                                                    @"code": @(processError.code),
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
            params.onBehalfOf = options[@"onBehalfOf"];
            params.transferDataDestination = options[@"transferDataDestination"];
        }

        [SCPTerminal.shared createPaymentIntent:params completion:onIntent];
    }
}

RCT_EXPORT_METHOD(createPaymentIntent:(NSDictionary *)options) {
    NSInteger amount = [RCTConvert NSInteger:options[@"amount"]];
    NSString *currency = [RCTConvert NSString:options[@"currency"]];

    SCPPaymentIntentParameters *params = [[SCPPaymentIntentParameters alloc] initWithAmount:amount currency:currency];

    NSInteger applicationFeeAmount = [RCTConvert NSInteger:options[@"applicationFeeAmount"]];
    
    if (applicationFeeAmount) {
        params.applicationFeeAmount = [NSNumber numberWithInteger:applicationFeeAmount];
        params.onBehalfOf = options[@"onBehalfOf"];
        params.transferDataDestination = options[@"transferDataDestination"];
    }

    [SCPTerminal.shared createPaymentIntent:params completion:^(SCPPaymentIntent * _Nullable intent_, NSError * _Nullable error) {
        intent = intent_;
        if (error) {
            [self sendEventWithName:@"paymentIntentCreation" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"paymentIntentCreation" body:@{@"intent": [self serializePaymentIntent:intent]}];
        }
    }];
}

RCT_EXPORT_METHOD(retrievePaymentIntent:(NSString *)clientSecret) {
    [SCPTerminal.shared retrievePaymentIntent:clientSecret completion:^(SCPPaymentIntent * _Nullable intent_, NSError * _Nullable error) {
        intent = intent_;
        if (error) {
            [self sendEventWithName:@"paymentIntentRetrieval" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"paymentIntentRetrieval" body:@{@"intent": [self serializePaymentIntent:intent]}];
        }
    }];
}

RCT_EXPORT_METHOD(collectPaymentMethod) {
    pendingCreatePaymentIntent = [SCPTerminal.shared collectPaymentMethod:intent delegate:self completion:^(SCPPaymentIntent * _Nullable collectedIntent, NSError * _Nullable error) {
        pendingCreatePaymentIntent = nil;
        if (error) {
            [self sendEventWithName:@"paymentMethodCollection" body:@{
                                                                    @"error": [error localizedDescription],
                                                                    @"code": @(error.code),
                                                                    @"intent": [self serializePaymentIntent:intent]
                                                                    }];
        } else {
            intent = collectedIntent;
            [self sendEventWithName:@"paymentMethodCollection" body:@{@"intent": [self serializePaymentIntent:intent]}];
        }
    }];
}

RCT_EXPORT_METHOD(processPayment) {
    [SCPTerminal.shared processPayment:intent completion:^(SCPPaymentIntent * _Nullable confirmedIntent, SCPProcessPaymentError * _Nullable error) {
        if (error) {
            [self sendEventWithName:@"paymentProcess" body:@{
                @"error": [error localizedDescription],
                @"code": @(error.code),
                @"declineCode": error.declineCode ? error.declineCode : @"",
                @"intent": [self serializePaymentIntent:intent]
                }];

        } else {
            intent = confirmedIntent;
            [self sendEventWithName:@"paymentProcess" body:@{@"intent": [self serializePaymentIntent:confirmedIntent]}];
        }
    }];
}

RCT_EXPORT_METHOD(cancelPaymentIntent) {
    [SCPTerminal.shared cancelPaymentIntent:intent completion:^(SCPPaymentIntent * _Nullable canceledIntent, NSError * _Nullable error) {
        if (error) {
            [self sendEventWithName:@"paymentIntentCancel" body:@{
                                                                    @"error": [error localizedDescription],
                                                                    @"code": @(error.code),
                                                                    @"intent": [self serializePaymentIntent:intent]
                                                                    }];

        } else {
            [self sendEventWithName:@"paymentIntentCancel" body:@{@"intent": [self serializePaymentIntent:canceledIntent]}];
        }
    }];
}

- (void)terminal:(SCPTerminal *)terminal didRequestReaderInput:(SCPReaderInputOptions)inputOptions {
    [self sendEventWithName:@"didRequestReaderInput" body:
     @{
       @"text": [SCPTerminal stringFromReaderInputOptions:inputOptions]
       }];
}

- (void)terminal:(SCPTerminal *)terminal didRequestReaderDisplayMessage:(SCPReaderDisplayMessage)displayMessage {
    NSString* const SCPReaderDisplayMessageToStringMap[] = {
        [SCPReaderDisplayMessageRetryCard] = @"RetryCard",
        [SCPReaderDisplayMessageInsertCard] = @"InsertCard",
        [SCPReaderDisplayMessageInsertOrSwipeCard] = @"InsertOrSwipeCard",
        [SCPReaderDisplayMessageSwipeCard] = @"SwipeCard",
        [SCPReaderDisplayMessageRemoveCard] = @"RemoveCard",
        [SCPReaderDisplayMessageMultipleContactlessCardsDetected] = @"MultipleContactlessCardsDetected",
        [SCPReaderDisplayMessageTryAnotherReadMethod] = @"TryAnotherReadMethod",
        [SCPReaderDisplayMessageTryAnotherCard] = @"TryAnotherCard"
    };

    [self sendEventWithName:@"didRequestReaderDisplayMessage"
          body: SCPReaderDisplayMessageToStringMap[displayMessage]];
}

- (void)terminal:(SCPTerminal *)terminal didReportReaderEvent:(SCPReaderEvent)event info:(NSDictionary *)info {
    lastReaderEvent = event;
    [self sendEventWithName:@"didReportReaderEvent" body:
     @{
       @"event": @(event),
       @"info": info ? info : @{}
       }];
}

- (void)terminal:(SCPTerminal *)terminal didReportLowBatteryWarning:(SCPTerminal *)terminal_ {
    [self sendEventWithName:@"didReportLowBatteryWarning" body:@{}];
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

- (void)terminal:(SCPTerminal *)terminal didReportUnexpectedReaderDisconnect:(SCPReader *)reader {
    [self sendEventWithName:@"didReportUnexpectedReaderDisconnect" body:[self serializeReader:reader]];
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

RCT_EXPORT_METHOD(abortInstallUpdate) {
    if (pendingInstallUpdate) {
        [pendingInstallUpdate cancel:^(NSError * _Nullable error) {
            if (error) {
                [self sendEventWithName:@"abortInstallUpdateCompletion" body:@{@"error": [error localizedDescription]}];
            } else {
                pendingInstallUpdate = nil;
                [self sendEventWithName:@"abortInstallUpdateCompletion" body:@{}];
            }
        }];
        return;
    }
    [self sendEventWithName:@"abortInstallUpdateCompletion" body:@{}];
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
