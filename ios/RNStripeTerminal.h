#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#else
#import "RCTBridgeModule.h"
#endif

#import <StripeTerminal/StripeTerminal.h>

@interface RNStripeTerminal : RCTEventEmitter <RCTBridgeModule, SCPConnectionTokenProvider> {

    SCPConnectionTokenCompletionBlock pendingConnectionTokenCompletionBlock;
}

@end
