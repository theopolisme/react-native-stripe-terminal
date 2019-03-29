#import "RNStripeTerminal.h"
#import <StripeTerminal/StripeTerminal.h>

@implementation RNStripeTerminal

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[
             @"requestConnectionToken"
            ];
}

- (void)fetchConnectionToken:(SCPConnectionTokenCompletionBlock)completion {
    pendingConnectionTokenCompletionBlock = completion;
    [self sendEventWithName:@"requestConnectionToken" body:@{}];
}

RCT_EXPORT_METHOD(setConnectionToken:(NSString *)token error:(NSError *)error) {
    if (pendingConnectionTokenCompletionBlock) {
        pendingConnectionTokenCompletionBlock(token, error);
        pendingConnectionTokenCompletionBlock = nil;
    }
}

RCT_EXPORT_METHOD(initialize) {
    NSLog(@"Initializing RNStripe");
    SCPTerminal.shared.logLevel = SCPLogLevelVerbose;
    [SCPTerminal setTokenProvider:self];
}

RCT_EXPORT_MODULE()

@end

