//
//  LoggerUtil.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri on 1/9/23.
//

import Foundation
import os


struct ScannerLogger {
    static func log(level: RCTLogLevel,
                    message: String,
                    _ file: String = #file,
                    _ lineNumber: Int = #line,
                    _ function: String = #function) {
      #if DEBUG
        RCTDefaultLogFunction(level, RCTLogSource.native, file, lineNumber as NSNumber, "RawQRCodeReader.\(function): \(message)")
      #endif
    }
}
