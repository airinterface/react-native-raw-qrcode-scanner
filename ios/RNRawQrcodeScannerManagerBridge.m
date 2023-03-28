#import <React/RCTViewManager.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
//
@interface RCT_EXTERN_MODULE(RNRawQrCodeScannerEventEmitter, RCTEventEmitter)
RCT_EXTERN_METHOD(supportedEvents)
@end

@interface RCT_EXTERN_MODULE(RNRawQrcodeScannerManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(cameraType, NSString)
RCT_EXPORT_VIEW_PROPERTY(scanEnabled, BOOL )
RCT_EXPORT_VIEW_PROPERTY(flashEnabled, BOOL )
RCT_EXPORT_VIEW_PROPERTY(samplingRateInMS, NSNumber )
//RCT_EXPORT_VIEW_PROPERTY(onScanned, RCTDirectEventBlock);
@end

