//
//  RNRawQrcodeScannerEmitter.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri on 2/2/23.
//

import Foundation

@objc(RNRawQrCodeScannerEventEmitter)
open class RNRawQrCodeScannerEventEmitter: RCTEventEmitter {
    public static var eventEmitter: RCTEventEmitter!

    override init() {
        super.init()
        RNRawQrCodeScannerEventEmitter.eventEmitter = self
    }

    static func dispatch(name: String, body: NSDictionary) {
        DispatchQueue.main.async {
            eventEmitter.sendEvent(withName: name, body: body)
        }
    }
    
    /// Base overide for RCTEventEmitter.
    /// - Returns: all supported events
    @objc open override func supportedEvents() -> [String] {
        return ["onScanned"]
    }

    @objc open override class func requiresMainQueueSetup() -> Bool {
         return false
    }
}
