package com.rawqrcodescanner.camera

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.barcode.common.Barcode
import com.rawqrcodescanner.R
import com.rawqrcodescanner.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias OnScannedCallbackType = (barcodes: List<ScannedBarcode>) -> Unit

class ScannerView(private var reactContext: ReactContext, private var onScanned: OnScannedCallbackType?) : FrameLayout(reactContext.applicationContext), LifecycleOwner {

  companion object {
    const val TAG = "CameraView"
    private val propsThatRequireSessionReconfiguration = arrayListOf("cameraType", "flashEnabled")
    private val cameraTypes:Map<String, Int> = mapOf(
      "back" to CameraSelector.LENS_FACING_BACK,
      "front" to CameraSelector.LENS_FACING_FRONT
    )

  }


  var cameraType: String  = "back"
  var flashEnabled: Boolean = false
  var scanEnabled: Boolean = true
    set( value ) {
      analyzer?.enabled = value
    }
  var samplingRateInMS: Long = QRCodeAnalyzer.DefaultSamplingRateInMS
    set( value ) {
      analyzer?.samplingRateInMS = value
    }

  private var camera: Camera? = null
  private var preview: Preview? = null
  private var mainCoroutineScope = CoroutineScope(Dispatchers.Main)

  private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val callBackExecutor:ScopedExecutor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
  private val lifecycleRegistry: LifecycleRegistry
  private var hostLifecycleState: Lifecycle.State
  private lateinit var analyzer: QRCodeAnalyzer
  private lateinit var viewFinder: PreviewView

  private val stateMutex = Mutex();
  private lateinit var cameraProvider: ProcessCameraProvider;
  private var processQRCode: Boolean = true;
  private var isMounted = false;
  private val inputRotation: Int
    get() {
      return reactContext.displayRotation
    }

  init {
    hostLifecycleState = Lifecycle.State.INITIALIZED
    lifecycleRegistry = LifecycleRegistry(this)
    reactContext.addLifecycleEventListener(object : LifecycleEventListener {
      override fun onHostResume() {
        try {
          Log.i(TAG, "#YF ---------------onHostResume 1")
          hostLifecycleState = Lifecycle.State.RESUMED
          Log.i(TAG, "#YF ---------------onHostResume 2")
          updateLifecycleState()
          Log.i(TAG, "#YF ---------------onHostResume 3")
        } catch( e: Exception ) {
          Log.i(TAG, "#YF ---------------onHostResume error " + e )
        }
      }
      override fun onHostPause() {
        hostLifecycleState = Lifecycle.State.CREATED
        updateLifecycleState()
      }
      override fun onHostDestroy() {
        hostLifecycleState = Lifecycle.State.DESTROYED
        callBackExecutor.shutdown()
        updateLifecycleState()
        cameraExecutor.shutdown()
        reactContext.removeLifecycleEventListener(this)
      }
    })
    Log.i(TAG,"#YF ---------------Initializing PreviewView")
    initializeAnalyzer()
    initView();

  }
  private fun initializeAnalyzer(){
    analyzer = QRCodeAnalyzer { barcodes ->
      if (!processQRCode || barcodes.count() == 0) {
        return@QRCodeAnalyzer
      }
      Log.d(TAG,"Got ${barcodes.count()} Barcodes")
      val resultBarcodes = barcodes.map { barcode ->
        try {
          processQRCodeValue(barcode)
        } catch (e: Throwable) {
          Log.d(TAG, "Exception processing barcode")
          null
        }
      }.filter { it -> ( it != null)}
      onBarcodeScanned( resultBarcodes as List<ScannedBarcode>  );
    }
  }
  private fun sendEvent(
    reactContext: ReactContext,
    eventName: String,
    params: WritableMap
  ) {
    callBackExecutor.let {
      reactContext
        .getJSModule(RCTDeviceEventEmitter::class.java)
        .emit(eventName, params)
    }
  }

  private fun onBarcodeScanned(scannedBarcode: List<ScannedBarcode> ) {
    if(scannedBarcode.isNotEmpty()) {
        val event = Arguments.createMap()
        val allRecognizedCodes = Arguments.createArray()
       scannedBarcode.forEach{ barcode ->
          allRecognizedCodes.pushMap( barcodeToMap(barcode) );
        }
        event.putArray( "results", allRecognizedCodes )
        Log.d(TAG, "sending result")
        mainCoroutineScope.launch {
          sendEvent(reactContext, "onScanned", event );
        }
    }
  }

  fun update(changedProps: ArrayList<String>) = viewFinder.post {
    mainCoroutineScope.launch {
      val shouldReconfigureSession = changedProps.containsAnyProp(propsThatRequireSessionReconfiguration)
      val shouldReconfigureFlash = shouldReconfigureSession || changedProps.contains("flashEnabled")
      if (changedProps.contains("scanEnabled")) {
        updateLifecycleState()
      }
      if (shouldReconfigureSession) {
        configureCameraSession()
      }
    }
  }
  private fun updateLifecycleState() {
    val lifecycleBefore = lifecycleRegistry.currentState
    if (hostLifecycleState == Lifecycle.State.RESUMED) {
      // Host Lifecycle (Activity) is currently active (RESUMED), so we narrow it down to the view's lifecycle
      if (isAttachedToWindow) {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
      } else {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
      }
    } else {
      // Host Lifecycle (Activity) is currently inactive (STARTED or DESTROYED), so that overrules our view's lifecycle
      lifecycleRegistry.currentState = hostLifecycleState
    }
    Log.d(TAG, "Lifecycle went from ${lifecycleBefore.name} -> ${lifecycleRegistry.currentState.name} ( isAttachedToWindow: $isAttachedToWindow)")
  }



  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    updateLifecycleState()
    cameraProvider?.let { it.unbindAll() }

  }


  override fun getLifecycle(): Lifecycle {
    return lifecycleRegistry
  }
  override fun onConfigurationChanged(newConfig: Configuration?) {
    super.onConfigurationChanged(newConfig)
    updateOrientation()
  }
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    updateLifecycleState()
    if (!isMounted) {
      isMounted = true
      updateViewBasedOnCameraPermission()
    }
  }

  private fun initView(): ScannerView{
    val view =  inflate(context.applicationContext, R.layout.preview_layout, this) as ScannerView;
    viewFinder = view.findViewById(R.id.view_finder);
    viewFinder.installHierarchyFitter() // If this is not called correctly, view finder will be black/blank
    return view;
  }

  private fun updateOrientation() {
    preview?.targetRotation = inputRotation
  }

  private fun hasCameraPermission(): Boolean {
    return (ContextCompat.checkSelfPermission(
      context.applicationContext,
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED);
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


  private fun setUpCamera() = viewFinder.post{
    mainCoroutineScope.launch {
      configureCameraSession()
    }
  }


  suspend fun pauseScan() {
    stateMutex.withLock {
      processQRCode = false;
    }
  }
  suspend fun resumeScan() {
    stateMutex.withLock {
      processQRCode = true;
    }
  }

  private fun getFacing( cameraFacing: String ):Int {
    return (cameraTypes.get(cameraFacing)?: cameraTypes.get("back")) as Int;
  }

  private suspend fun configureCameraSession(){
    cameraProvider = ProcessCameraProvider.getInstance(reactContext).await()
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

    val imageAnalyzer = buildImageAnalyzer(aspectRatio, rotation)
    cameraProvider.unbindAll()
    // CameraSelector
    val cameraSelector =
      CameraSelector.Builder().requireLensFacing(getFacing( cameraType )).build()

    try {
      // A variable number of use-cases can be passed here -
      // camera provides access to CameraControl & CameraInfo
      camera = cameraProvider.bindToLifecycle(
        this, cameraSelector, preview, imageAnalyzer
      )

      if( camera?.cameraInfo?.hasFlashUnit() == true ) {
        camera?.cameraControl?.enableTorch(flashEnabled)
      }
      preview!!.setSurfaceProvider(viewFinder.surfaceProvider)
      // Attach the viewfinder's surface provider to preview use case
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
    } catch ( arg: Exception ) {
      Log.e(
        TAG,
        arg.toString()
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
          analyzer
        )
      }
  }

  private fun processQRCodeValue(barcode: Barcode): ScannedBarcode{
    return when (barcode.valueType) {
      Barcode.TYPE_URL -> {
        barcode.rawValue?.let {
          ScannedBarcode(
            type = ScannedType.URL,
            url = it,
            rawbytes = null,
            text = barcode.rawValue
          )
        } ?: throw java.lang.IllegalArgumentException("Barcode URL missing")
      }
      Barcode.TYPE_TEXT -> {
        when (val value = barcode.rawValue) {
          null, QRCodeAnalyzer.UnknownEncodingMessage -> {
            barcode.rawBytes?.let {
              ScannedBarcode(
                type = ScannedType.RAW,
                url = null,
                rawbytes = it,
                text = barcode.rawValue
              )
            }
              ?: throw java.lang.IllegalArgumentException("Barcode data missing")
          }
          else -> ScannedBarcode(
            type = ScannedType.TEXT,
            url = null,
            rawbytes = barcode.rawBytes,
            text = barcode.rawValue
          )
        }
      }
      else -> throw java.lang.IllegalArgumentException("Unknown barcode type ${barcode.valueType}")
    }
  }

}
