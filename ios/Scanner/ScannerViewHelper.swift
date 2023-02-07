//
//  ScannerViewHelper.swift
//  react-native-raw-qrcode-scanner
//  This helps bridging Scanner to React Native View Model
//  Created by Yuri on 1/27/23.
//

import Foundation

@objc(ScannerViewHelper)
class ScannerViewHelper: NSObject {
    var controller: ScannerController!;
    
    var scannerView: ScannerView {
        get {
            return self.controller.previewView;
        }
    }
        
    override init() {
        super.init()
        controller = ScannerController();
    };

}

