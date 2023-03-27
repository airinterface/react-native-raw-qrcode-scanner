package com.rawqrcodescanner.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.collections.ArrayList

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
    const val DefaultSamplingRateInMS = 300L;
  }
  private var lastFrameProcessorCall = System.currentTimeMillis()

  private val scanner = run {
      val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
      BarcodeScanning.getClient(options)
    }
  var enabled:Boolean = true;
  var samplingRateInMS: Long = DefaultSamplingRateInMS;
  private val listeners = ArrayList<QRCodeListener>().apply { listener?.let { add(it) } }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

      val now = System.currentTimeMillis()
      if ( !enabled ) {
        return
      }
      val passedTime = now - lastFrameProcessorCall
      if (passedTime  < samplingRateInMS ) {
        imageProxy.close()
        return
      }
        // If there are no listeners attached, we don't need to perform analysis
      if (listeners.isEmpty()) {
        imageProxy.close()
        return
      }

      val mediaImage = imageProxy.image
      if (mediaImage != null && mediaImage.height > 0 && mediaImage.width > 0 ) {
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
          lastFrameProcessorCall = now
      } else {
        imageProxy.close()
      }
    }
}

