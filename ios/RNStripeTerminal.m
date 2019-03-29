#import "RNStripeTerminal.h"
#import <React/RCTConvert.h>
#import <StripeTerminal/StripeTerminal.h>

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
             @"requestConnectionToken",
             @"readersDiscovered",
             @"readerConnection",
             @"paymentIntentCreation",
             @"didBeginWaitingForReaderInput",
             @"didRequestReaderInputPrompt"
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
        [data addObject:@{
                          @"batteryLevel": reader.batteryLevel ? reader.batteryLevel : @(0),
                          @"deviceType": reader.deviceType ? @(reader.deviceType) : @(-1),
                          @"serialNumber": reader.serialNumber ? reader.serialNumber : @"",
                          @"deviceSoftwareVersion": reader.deviceSoftwareVersion ? reader.deviceSoftwareVersion : @""
                          }];
    }];
    
    [self sendEventWithName:@"readersDiscovered" body:data];
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
    SCPTerminal.shared.logLevel = SCPLogLevelVerbose;
}

RCT_EXPORT_METHOD(discoverReaders:(NSInteger *)deviceType method:(NSInteger *)method) {
    SCPDiscoveryConfiguration *config = [[SCPDiscoveryConfiguration alloc] initWithDeviceType:(SCPDeviceType)deviceType method:(SCPDiscoveryMethod)method];
    [SCPTerminal.shared discoverReaders:config delegate:self completion:^(NSError * _Nullable error) {
        // fixme: add handler for discovery error?
    }];
}

RCT_EXPORT_METHOD(connectReader:(NSString *)serialNumber ) {
    int readerIndex = [readers indexOfObjectPassingTest:^(SCPReader *reader, NSUInteger idx, BOOL *stop) {
        return [reader.serialNumber isEqualToString:serialNumber];
    }];
    
    [SCPTerminal.shared connectReader:readers[readerIndex] completion:^(SCPReader * _Nullable reader_, NSError * _Nullable error) {
        reader = reader_;
        if (error) {
            [self sendEventWithName:@"readerConnection" body:@{@"error": [error localizedDescription]}];
        } else {
            [self sendEventWithName:@"readerConnection" body:@{}];
        }
    }];
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

RCT_EXPORT_METHOD(createPaymentIntent:(NSDictionary *)options) {
    NSInteger amount = [RCTConvert NSInteger:options[@"amount"]];
    NSString *currency = [RCTConvert NSString:options[@"currency"]];
    
    SCPPaymentIntentParameters *params = [[SCPPaymentIntentParameters alloc] initWithAmount:amount currency:currency];
    
    NSLog(@"creating payment intent...");
    [SCPTerminal.shared createPaymentIntent:params completion:^(SCPPaymentIntent * _Nullable createdIntent, NSError * _Nullable creationError) {
        if (creationError) {
            [self sendEventWithName:@"paymentIntentCreation" body:@{@"error": [creationError localizedDescription]}];
            
        } else {
            NSLog(@"collecting payment method...");
            [SCPTerminal.shared collectPaymentMethod:createdIntent delegate:self completion:^(SCPPaymentIntent * _Nullable collectedIntent, NSError * _Nullable collectionError) {
                if (collectionError) {
                    [self sendEventWithName:@"paymentIntentCreation" body:@{@"error": [collectionError localizedDescription]}];
                    
                } else {
                    NSLog(@"confirming payment intent...");
                    [SCPTerminal.shared confirmPaymentIntent:collectedIntent completion:^(SCPPaymentIntent * _Nullable confirmedIntent, SCPConfirmError * _Nullable confirmingError) {
                        if (confirmingError) {
                            [self sendEventWithName:@"paymentIntentCreation" body:@{@"error": [confirmingError localizedDescription]}];
                            
                        } else {
                            [self sendEventWithName:@"paymentIntentCreation" body:@{@"intent": [self serializePaymentIntent:confirmedIntent]}];
                        }
                    }];
                }
            }];
        }
    }];
}

- (void)terminal:(SCPTerminal *)terminal didBeginWaitingForReaderInput:(SCPReaderInputOptions)inputOptions {
    NSLog(@"did wait in...");
    [self sendEventWithName:@"didBeginWaitingForReaderInput" body:
     @{
       @"text": [SCPTerminal stringFromReaderInputOptions:inputOptions]
       }];
}

- (void)terminal:(SCPTerminal *)terminal didRequestReaderInputPrompt:(SCPReaderInputPrompt)inputPrompt {
    NSLog(@"did req in...");
    [self sendEventWithName:@"didRequestReaderInputPrompt" body:
     @{
       @"text": [SCPTerminal stringFromReaderInputPrompt:inputPrompt]
       }];
}

RCT_EXPORT_MODULE()

@end

