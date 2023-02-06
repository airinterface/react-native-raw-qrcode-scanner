//
//  ScannerController+LifeCycle.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri on 1/25/23.
//

import Foundation
import AVKit

extension ScannerController {
    override func viewDidLoad() {
      super.viewDidLoad()
    }
    
    override func viewWillAppear(_ animated: Bool) {
      super.viewWillAppear(animated)
      if isCaptureSessionConfigured {
        if !captureSession.isRunning {
          captureSession.startRunning()
        }
      } else {
        captureSession = AVCaptureSession()
        configurePreviewView();
        configureCaptureSession()
        isCaptureSessionConfigured = true
        captureSession.startRunning()
        previewView.updateVideoOrientationForDeviceOrientation()
      }
      
    }
    
    override func viewWillDisappear(_ animated: Bool) {
      super.viewWillDisappear(animated)
      
      if captureSession.isRunning {
        captureSession.stopRunning()
      }
    }

}
