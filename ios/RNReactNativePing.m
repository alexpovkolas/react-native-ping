
#import "RNReactNativePing.h"
#import "GBPing.h"
#import "LHNetwork.h"
#import "LHDefinition.h"

@interface RNReactNativePing ()
@property (nonatomic, strong) dispatch_queue_t queue;
@property (nonatomic, strong) GBPing* ping;

@end

@implementation RNReactNativePing


RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"PingEvent"];
}

- (dispatch_queue_t)methodQueue
{
    if (!_queue) {
        _queue = dispatch_queue_create("com.pomato.React.RNReactNativePing", DISPATCH_QUEUE_SERIAL);
    }
    return _queue;
}

RCT_EXPORT_METHOD(start:(NSString *)host
                  count:(NSNumber *)count
                  option:(NSDictionary *)option
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    self.ping = [[GBPing alloc] init];
    self.ping.host = host;
    
    NSNumber *pingPeriod = option[@"pingPeriod"];
    if (pingPeriod) {
        self.ping.pingPeriod = [pingPeriod doubleValue];
    }
    
    NSNumber *timeout = option[@"timeout"];
    if (timeout) {
        self.ping.timeout = [timeout doubleValue];
    }
    
    NSNumber *payloadSize = option[@"payloadSize"];
    if (payloadSize) {
        self.ping.payloadSize = [payloadSize intValue];
    }
    
    NSNumber *ttl = option[@"ttl"];
    if (ttl) {
        self.ping.ttl = [ttl intValue];
    }
        

    __weak RNReactNativePing *weakSelf = self;
    [self.ping setupWithBlock:^(BOOL success, NSError *_Nullable err) {
        if (!success) {
            reject(@(err.code).stringValue,err.domain,err);
            return;
        }

        [weakSelf.ping startPingingWithBlock:^(GBPingSummary *summary) {
            if (!weakSelf.ping) {
                return;
            }
            
            [self sendEventWithName:@"PingEvent" body:@{@"sequenceNumber": @(summary.sequenceNumber),
                                                        @"ttl": @(summary.ttl),
                                                        @"rtt": @(summary.rtt),
                                                        @"status": [RNReactNativePing stringStatus: summary.status]}];
            
        } fail:^(NSError *_Nonnull error) {
            if (!weakSelf.ping) {
                return;
            }
            reject(@(error.code).stringValue,error.domain,error);
            [weakSelf.ping stop];
            weakSelf.ping = nil;
        }];
        
        resolve(@(YES));
    }];
}

RCT_EXPORT_METHOD(stop) {
    [self.ping stop];
    self.ping = nil;
}

+ (NSString*)stringStatus: (GBPingStatus) status {
    switch (status) {
        case GBPingStatusPending:
            return @"Pending";
        case GBPingStatusSuccess:
            return @"Success";
        case GBPingStatusFail:
            return @"Fail";
    }
}

@end
