package com.rawqrcodescanner.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.facebook.react.bridge.ReactContext
import com.rawqrcodescanner.R
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.rawqrcodescanner.utils.displayRotation;
import com.rawqrcodescanner.utils.aspectRatio;
import android.content.Context;
import androidx.camera.core.*
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.barcode.common.Barcode
import com.rawqrcodescanner.utils.ScopedExecutor
import kotlinx.coroutines.sync.withLock

typealias onScannedCallback = (barcodes: List<ScannedBarcode>) -> Unit

class ScannerFragment(
  private var onScanned:onScannedCallback?
): Fragment() {

  companion object {
    const val TAG = "CameraView"
  }
  private var camera: Camera? = null
  private var preview: Preview? = null
  private var imageCapture: ImageCapture? = null
  private var imageAnalyzer: ImageAnalysis? = null
  private lateinit var cameraExecutor: ExecutorService
  private val callBackExecutor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

  private var cameraProvider: ProcessCameraProvider? = null
  private lateinit var viewFinder: PreviewView

  private val stateMutex = Mutex();
  private val cameraProviderMutex = Mutex();
  private var processQRCode: Boolean = true;
  private val inputRotation: Int
    get() {
      return reactContext.displayRotation
    }
  private val reactContext: ReactContext
    get() = context as ReactContext


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.preview_layout, container, false)
    view?.let {
      viewFinder = view.findViewById(R.id.viewFinder)
    }
    return view;
  }

  override fun onResume() {
    super.onResume()
    updateViewBasedOnCameraPermission()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    cameraExecutor = Executors.newSingleThreadExecutor()
  }

  private fun hasCameraPermission(): Boolean {
    return (ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.CAMERA
    ) != PackageManager.PERMISSION_GRANTED);
  }

  private fun updateViewBasedOnCameraPermission() {
    if (hasCameraPermission()) {
      setUpCamera()
    } else {
      Log.d(
        TAG,
        "camera permission isn't available"
      )
    }
  }


  private fun setUpCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    cameraProviderFuture.addListener(
      Runnable {
        val provider = cameraProviderFuture.get()
        cameraProvider = provider
        configureCameraSession(provider)
      },
      ContextCompat.getMainExecutor(requireContext())
    )
  }
  private suspend fun pauseScan() {
    stateMutex.withLock {
      processQRCode = false;
    }
  }
  private suspend fun resumeScan() {
    stateMutex.withLock {
      processQRCode = true;
    }
  }



  private fun configureCameraSession(cameraProvider: ProcessCameraProvider) {

    // Set up preview stream
    val aspectRatio = aspectRatio(
      viewFinder.height,
      viewFinder.width
    ) // flipped because it's in sensor orientation.
    preview = Preview.Builder()
      .setTargetRotation(inputRotation)
      .setTargetAspectRatio(aspectRatio)
      .build()
    val rotation = viewFinder.display.rotation

    // Set up preview stream
    val imageCaptureBuilder = ImageCapture.Builder()
      .setTargetRotation(viewFinder.display.rotation)
      .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

    val imageAnalyzer = buildImageAnalyzer(aspectRatio, rotation)
    cameraProvider.unbindAll()
    // CameraSelector
    val cameraSelector =
      CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

    try {
      // A variable number of use-cases can be passed here -
      // camera provides access to CameraControl & CameraInfo
      camera = cameraProvider.bindToLifecycle(
        this, cameraSelector, preview, imageAnalyzer
      )

      // Attach the viewfinder's surface provider to preview use case
      preview?.setSurfaceProvider(viewFinder.surfaceProvider)
    } catch (exc: IllegalStateException) {
      Log.e(
        TAG,
        "Use case has already been bound to another lifecycle or method is not called on main thread."
      )
    } catch (arg: IllegalArgumentException) {
      Log.e(
        TAG,
        "Camera selector is unable to resolve a camera to be used for the given use cases."
      )
    }
  }

  private fun buildImageAnalyzer(screenAspectRatio: Int, rotation: Int): ImageAnalysis {
    return ImageAnalysis.Builder()
      // We request aspect ratio but no resolution
      .setTargetAspectRatio(screenAspectRatio)
      // Set initial target rotation, we will have to call this again if rotation changes
      // during the lifecycle of this use case
      .setTargetRotation(rotation)
      .build()
      // The analyzer can then be assigned to the instance
      .also { imageAnalysis ->

        imageAnalysis.setAnalyzer(
          cameraExecutor,
          QRCodeAnalyzer { barcodes ->
            if (!processQRCode || barcodes.count() == 0) {
              return@QRCodeAnalyzer
            }
            Log.d(TAG,"Got ${barcodes.count()} Barcodes")
            barcodes.forEach { barcode ->

              val scannedBarcode = try {
                processedBarcodeValue(barcode)
              } catch (e: Throwable) {
                Log.d(TAG, "Exception processing barcode")
                null
              }
            }
          }
        )
      }
  }

  private fun processedBarcodeValue(barcode: Barcode): ScannedBarcode {
    return when (barcode.valueType) {
      Barcode.TYPE_URL -> {
        barcode.rawValue?.let {
          ScannedBarcode.Data( type=ScannedType.URL, url=it, rawbytes=null, text = barcode.rawValue,)
        } ?: throw java.lang.IllegalArgumentException("Barcode URL missing")
      }
      Barcode.TYPE_TEXT -> {
        when (val value = barcode.rawValue) {
          null, QRCodeAnalyzer.UnknownEncodingMessage -> {
            barcode.rawBytes?.let {
              ScannedBarcode.Data( type=ScannedType.RAW, url=null, rawbytes=it, text = barcode.rawValue,)
            }
              ?: throw java.lang.IllegalArgumentException("Barcode data missing")
          }
          else -> ScannedBarcode.Data( type=ScannedType.TEXT, url=null, rawbytes=barcode.rawBytes, text = barcode.rawValue,)
        }
      }
      else -> throw java.lang.IllegalArgumentException("Unknown barcode type ${barcode.valueType}")
    }
  }

}
