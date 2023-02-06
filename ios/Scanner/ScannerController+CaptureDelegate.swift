//
//  ScannerController+CaptureDelegate.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri on 1/25/23.
//

import Foundation
import AVKit

extension ScannerController : AVCaptureMetadataOutputObjectsDelegate {

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        if self.scanEnabled {
            guard !metadataObjects.isEmpty else {
                return;
            }
            var readBarcodes: [NSDictionary] = []
            metadataObjects.forEach {
                let qr = $0 as? AVMetadataMachineReadableCodeObject
                if qr != nil  {
                    let strVal = qr!.stringValue
                    if( strVal != nil ) {
                        let rawVal =   ( qr!.value(forKeyPath: "_internal.basicDescriptor")! as! NSDictionary ) ["BarcodeRawData"] as? NSData
                        let rawData = [UInt8](rawVal!);

                        let type =   ( qr!.value(forKeyPath: "_internal.basicDescriptor")! as! NSDictionary )["BarcodeType"] as? NSString

                        readBarcodes.append([
                            "text": strVal! as NSString,
                            "rawBytes": rawData as [UInt8],
                            "type": type! as NSString ] as NSDictionary )
                    }
                }
            }
            if readBarcodes.count > 0 {
                self.onRead( ["results": readBarcodes] as NSDictionary);
            }
            
        }
        
    }

    private func onRead(_ barcodes: NSDictionary ) {
        ( self.view as! ScannerView ).onRead( barcodes )
    }
    
    private func removeReaderFeature() {
        if metadataOutput != nil {
            captureSession.removeOutput(metadataOutput);
            metadataOutput = nil;
        }
    }
    
    internal func setupReaderFeature() {
//        if metadataOutput == nil {
            metadataOutput = AVCaptureMetadataOutput();
            
            if (captureSession.canAddOutput(metadataOutput)) {
                captureSession.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                metadataOutput.metadataObjectTypes = [.qr]  // For QRCode video acquisition
            }
//        }
 
    }

}
