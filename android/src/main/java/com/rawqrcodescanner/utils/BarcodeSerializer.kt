package com.rawqrcodescanner.utils

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.rawqrcodescanner.camera.ScannedBarcode

fun barcodeToMap(barcode: ScannedBarcode): WritableMap {
  val map = Arguments.createMap()
  val rawData = Arguments.createArray()

  barcode?.rawbytes?.forEach { byte ->
    rawData.pushInt(byte.toInt())
  }
  map.putArray("rawBytes", rawData)
  map.putString("type", barcode.type.type)
  map.putString("text", barcode.text)
  map.putString("url", barcode.url)
  return map
}
