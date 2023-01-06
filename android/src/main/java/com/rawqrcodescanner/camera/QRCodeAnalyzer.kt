package com.rawqrcodescanner.camera

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

typealias QRCodeListener = (barcodes: List<Barcode>) -> Unit

enum class ScannedType( val type: String ) {
  URL( "URL"),
  TEXT("TEXT"),
  RAW( "RAW"),
}

class ScannedBarcode ( val type: ScannedType,  val rawbytes: ByteArray?, val text: String?, val url:String? ) {
}


class QRCodeAnalyzer (listener: QRCodeListener? = null) : ImageAnalysis.Analyzer{
  companion object  {
    const val TAG = "QRCodeAnalyzer"
    const val UnknownEncodingMessage = "Unknown encoding"
  }
  private var lastFrameProcessorCall = System.currentTimeMillis()
  private val frameProcessorFps = 30;
  private val scanner = run {
      val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
      BarcodeScanning.getClient(options)
    }
  var enabled:Boolean = true;
  private val listeners = ArrayList<QRCodeListener>().apply { listener?.let { add(it) } }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

      val now = System.currentTimeMillis()
      val intervalMs = (1.0 / frameProcessorFps) * 1000.0
      Log.d(TAG, ".")
      if ( !enabled ) {
        return
      }
      if (now - lastFrameProcessorCall < intervalMs) {
        imageProxy.close()
        return
      }
        // If there are no listeners attached, we don't need to perform analysis
      if (listeners.isEmpty()) {
        imageProxy.close()
        return
      }

      Log.d(TAG, "..Processing")
      val mediaImage = imageProxy.image
      if (mediaImage != null) {
        val image =
          InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        // Pass image to an ML Kit Vision API
        // ...
        scanner.process(image)
          .addOnSuccessListener { barcodes ->
            // Task completed successfully
            // ...
            listeners.forEach { it(barcodes) }
            imageProxy.close()
          }
          .addOnFailureListener {
            // Task failed with an exception
            // ...
            listeners.forEach { it(emptyList()) }
            imageProxy.close()
          }
      } else {
        imageProxy.close()
      }
      lastFrameProcessorCall = now
    }
}

