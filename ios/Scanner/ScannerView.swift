//
//  ScannerView.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri on 1/9/23.
//

import Foundation
import os
import AVKit
import UIKit


@objc(ScannerView)
class ScannerView : UIView {
    private var controller: ScannerController!
    
    @objc var cameraType: String = "" {
      didSet {
          self.controller?.setProp( propName:"cameraType", propValue: cameraType);
      }
    }
    @objc var scanEnabled:Bool = true {
        didSet {
            self.controller?.setProp( propName: "scanEnabled", propValue: "", propBoolean: scanEnabled )
        }
    }

    @objc var flashEnabled:Bool = true {
        didSet {
            self.controller?.setProp( propName: "flashEnabled", propValue: "", propBoolean: flashEnabled )
        }
    }
    @objc var onScanned:RCTDirectEventBlock?;

    override public final func didSetProps(_ changedProps: [String]!) {
        self.controller?.updateFromProp();
    }
    

    var videoPreviewLayer: AVCaptureVideoPreviewLayer {
      return layer as! AVCaptureVideoPreviewLayer
    }
    
    var session: AVCaptureSession? {
      get { return videoPreviewLayer.session }
      set { videoPreviewLayer.session = newValue }
    }
    
    
    
    override class var layerClass: AnyClass {
      return AVCaptureVideoPreviewLayer.self
    }
    
    private var orientationMap: [UIDeviceOrientation : AVCaptureVideoOrientation] = [
      .portrait : .portrait,
      .portraitUpsideDown : .portraitUpsideDown,
      .landscapeLeft : .landscapeRight,
      .landscapeRight : .landscapeLeft,
    ]

    convenience init( _ _controller: ScannerController ){
        let dummyFrame = CGRect(x: 0, y: 0, width: 100, height: 100)
        self.init(frame: dummyFrame )
        controller = _controller;
        ScannerLogger.log(level: .info, message: "Controller attached.")
    }
    override init( frame: CGRect ) {
        super.init(frame: frame)
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    

    
    func updateVideoOrientationForDeviceOrientation() {
      if let videoPreviewLayerConnection = videoPreviewLayer.connection {
        let deviceOrientation = UIDevice.current.orientation
        guard let newVideoOrientation = orientationMap[deviceOrientation], deviceOrientation.isPortrait || deviceOrientation.isLandscape else {
          return
        }
        videoPreviewLayerConnection.videoOrientation = newVideoOrientation
      }
    }

    
    override func didMoveToWindow() {
        super.didMoveToWindow()
        resize();
    }
    
    func resize(){
        videoPreviewLayer.videoGravity = .resizeAspectFill
        videoPreviewLayer.frame = layer.bounds
    }

    func onRead(_ barcodes: NSDictionary) {
        RNRawQrCodeScannerEventEmitter.dispatch(name: "onScanned", body:barcodes )
    }

    
}
