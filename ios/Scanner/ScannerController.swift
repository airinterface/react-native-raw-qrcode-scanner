//
//  ScannerCodeScannerController.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri on 1/9/23.
//

import Foundation
import UIKit
import AVKit
import UIKit
import Vision

struct ScannerParams {
    
}


class ScannerController : UIViewController {

    var previewView: ScannerView!
    var reconfigureNeeded: Bool = true
    var scanEnabled: Bool = true
    var currentDevice: AVCaptureDevice? = nil
    var torchMode: AVCaptureDevice.TorchMode = AVCaptureDevice.TorchMode.on
    var cameraFacing: AVCaptureDevice.Position = AVCaptureDevice.Position.back
    var videoInput: AVCaptureInput?;
    var processTimer: Timer? = nil;
    var timerProcessing: Bool = false
    @objc public static let frameProcessorQueue = DispatchQueue(
        label: "airinterface/RawQRCodeScanner",
        qos: .userInteractive,
        attributes: [],
        autoreleaseFrequency: .inherit,
        target: nil)

    // if this view controller is loaded from a storyboard, imageURL will be nil
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    init() {
        super.init(nibName: nil, bundle: nil)
        previewView = ScannerView( self )
        self.view = previewView
        self.view.backgroundColor = .yellow
        captureSession = AVCaptureSession()
    }
        
    internal var captureSession: AVCaptureSession!
    
    var metadataOutput: AVCaptureMetadataOutput!
    
    var isCaptureSessionConfigured = false
    var videoPreviewLayer: AVCaptureVideoPreviewLayer? = nil

    internal func configurePreviewView(){
        videoPreviewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        if let videoPreviewLayer = videoPreviewLayer {
          videoPreviewLayer.frame = previewView.bounds
          videoPreviewLayer.videoGravity = .resizeAspectFill
          previewView.layer.addSublayer(videoPreviewLayer)
        }
        previewView.clipsToBounds = true

    }
    
    
    func getDefaultDeviceFor(position: AVCaptureDevice.Position ) -> AVCaptureDevice? {
        var defaultVideoDevice: AVCaptureDevice? = nil
        if let dualCameraDevice = AVCaptureDevice.default(.builtInDualCamera, for: AVMediaType.video, position: position) {
            defaultVideoDevice = dualCameraDevice
        }

        else if let wideCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: AVMediaType.video, position: position) {
            defaultVideoDevice = wideCameraDevice
        }
        else if let frontCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: AVMediaType.video, position: .front) {
            defaultVideoDevice = frontCameraDevice
        }
        else if let backCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: AVMediaType.video, position: .back) {
            defaultVideoDevice = backCameraDevice
        }

        return defaultVideoDevice;
    }

    
    
    internal func configureCaptureSession() {

        captureSession.beginConfiguration()

        ScannerLogger.log(level: .info, message: "Initializing Camera with device \(cameraFacing)...")

    
        previewView.session = captureSession

        if let captureDevice = self.getDefaultDeviceFor(position: cameraFacing) {
            do {
                currentDevice = captureDevice;

                let  _videoInput = try AVCaptureDeviceInput(device: captureDevice );
                captureSession.canAddInput(_videoInput)

                if let videoInput = videoInput {
                    captureSession.removeInput(videoInput)
                    self.videoInput = nil
                }
                captureSession.addInput(_videoInput)
                videoInput = _videoInput;
            } catch {
              return
            }
        }else {
            return
        }

        setupReaderFeature();
        captureSession.commitConfiguration()
        reconfigureNeeded = false;
    }
    
    
    
    private func updateFlash(_ on: Bool ){
        let _flashMode = (on == true) ? AVCaptureDevice.TorchMode.on : AVCaptureDevice.TorchMode.off;
        if( _flashMode != self.torchMode ) {
            self.torchMode = _flashMode
            self.updateFlashMode();
//            self.reconfigureNeeded = true;
        }
    }
    
    private func updateFlashMode(){
        if currentDevice != nil {
            let captureDevice = currentDevice!
            if captureDevice.hasTorch {
                do {
                  try captureDevice.lockForConfiguration()
                  if torchMode == .on {
                      try captureDevice.setTorchModeOn(level: AVCaptureDevice.maxAvailableTorchLevel.significand)
                      captureDevice.torchMode = .on
                  } else {
                      captureDevice.torchMode = .off

                  }
                  captureDevice.unlockForConfiguration()
                } catch let error as NSError {
                    ScannerLogger.log(level: .info, message: "Error setting torch mode \(error)")
                    return
                }
            }
        } else {
            ScannerLogger.log(level: .info, message: "No Curent Device")

        }
    }
    
    
    private func updateScanEnabled( _ enable: Bool ) {
        if( enable != self.scanEnabled ) {
            self.scanEnabled = enable
        }
    }
    

    
    private func updateCameraType(_ cameraType: String ){
        let _cameraFacing = (cameraType == "front" ) ? AVCaptureDevice.Position.front : AVCaptureDevice.Position.back;
        if( _cameraFacing != self.cameraFacing ) {
            self.cameraFacing = _cameraFacing
            self.reconfigureNeeded = true;
        }
    }
    
    
    internal func setProp( propName: String, propValue: String, propBoolean: Bool = false ) {
        switch( propName ){
        case "cameraType":
            self.updateCameraType( propValue );
        case "flashEnabled":
            self.updateFlash( propBoolean );
        case "scanEnabled":
            self.updateScanEnabled( propBoolean );
        default: break
        }
    }
    
    internal func updateFromProp() {
        if( self.reconfigureNeeded ) {
            self.configureCaptureSession();
        }
    }
        
    internal func scanImage(cgImage: CGImage) {
      let barcodeRequest = VNDetectBarcodesRequest(completionHandler: { request, error in
        self.reportResults(results: request.results)
      })
      
      let handler = VNImageRequestHandler(cgImage: cgImage, options: [.properties : ""])
      
      guard let _ = try? handler.perform([barcodeRequest]) else {
        return print("Could not perform barcode-request!")
      }
    }
    
    private func reportResults(results: [Any]?) {
      // Loop through the found results
      print("Barcode observation")

      guard let results = results else {
        return print("No results found.")
      }

      print("Number of results found: \(results.count)")

      for result in results {
        
        // Cast the result to a barcode-observation
        if let barcode = result as? VNBarcodeObservation {
          
          if let payload = barcode.payloadStringValue {
            print("Payload: \(payload)")
          }
          
          // Print barcode-values
          print("Symbology: \(barcode.symbology.rawValue)")
          
          if let desc = barcode.barcodeDescriptor as? CIQRCodeDescriptor {
            let content = String(data: desc.errorCorrectedPayload, encoding: .utf8)
            
            // FIXME: This currently returns nil. I did not find any docs on how to encode the data properly so far.
            print("Payload: \(String(describing: content))")
            print("Error-Correction-Level: \(desc.errorCorrectionLevel)")
            print("Symbol-Version: \(desc.symbolVersion)")
          }
        }
      }
    }


}
