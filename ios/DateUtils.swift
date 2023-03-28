//
//  DateUtils.swift
//  react-native-raw-qrcode-scanner
//
//  Created by Yuri Fukuda on 3/28/23.
//

import Foundation

extension Date {
    var nowInMillSec: Int64 {
        Int64((self.timeIntervalSince1970 * 1000.0).rounded())
    }
}
