#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#else
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"
#endif

#import <StripeTerminal/StripeTerminal.h>

@interface RNStripeTerminal : RCTEventEmitter <RCTBridgeModule, SCPConnectionTokenProvider, SCPDiscoveryDelegate, SCPReaderInputDelegate, SCPTerminalDelegate> {

    NSArray<SCPReader *> *readers;
    SCPReader *reader;
    SCPConnectionTokenCompletionBlock pendingConnectionTokenCompletionBlock;
}

@end
