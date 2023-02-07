
@objc(RNRawQrcodeScannerManager)
class RNRawQrcodeScannerManager : RCTViewManager {

  override func view() -> (ScannerView) {
      let scannerViewHelper =  ScannerViewHelper();
      return scannerViewHelper.scannerView;
  }

  @objc override static func requiresMainQueueSetup() -> Bool {
    return false
  }

}


