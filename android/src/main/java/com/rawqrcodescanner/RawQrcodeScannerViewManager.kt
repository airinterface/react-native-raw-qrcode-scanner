package com.rawqrcodescanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.rawqrcodescanner.camera.ScannedBarcode
import com.rawqrcodescanner.camera.ScannerView
import com.rawqrcodescanner.utils.installHierarchyFitter


class RawQrcodeScannerViewManager : ViewGroupManager<LinearLayout>()  {
  companion object {
    const val REACT_CLASS = "RNRawQrcodeScanner";

    const val TAG = "RNRawQrcodeScanner"

    fun parsePermissionStatus(status: Int): String {
      return when (status) {
        PackageManager.PERMISSION_DENIED -> "denied"
        PackageManager.PERMISSION_GRANTED -> "authorized"
        else -> "not-determined"
      }
    }

    val scannerPropUpdate: HashMap<ScannerView, ArrayList<String>> = HashMap()
    private fun updateScannerProps(view: ScannerView, changedProp: String) {
      if (scannerPropUpdate[view] == null) {
        scannerPropUpdate[view] = ArrayList()
      }
      scannerPropUpdate[view]!!.add(changedProp)
    }
  }

  private lateinit var scannerView: ScannerView;
  private lateinit var rootView: LinearLayout;
  private lateinit var context: ThemedReactContext;
  override fun getName() = REACT_CLASS


  override fun createViewInstance(reactContext: ThemedReactContext): LinearLayout {
    context = reactContext;
    val inflater = LayoutInflater.from( reactContext as Context)
    rootView = inflater.inflate( R.layout.parent_view_layout, null, false) as LinearLayout;
    rootView.setLayoutParams(
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    )
    setupScannerView(context);
    return rootView
  }

  override fun onAfterUpdateTransaction(view: LinearLayout) {
    super.onAfterUpdateTransaction(view)
    val changedProps = scannerPropUpdate[scannerView] ?: ArrayList()
    scannerView.update(changedProps)
    scannerPropUpdate.remove(scannerView)
    rootView.x = 0f;
    rootView.y = 0f;
    rootView.installHierarchyFitter()
  }


  fun onScannedCallback( barcode: List<ScannedBarcode> ){
  }

  fun setupScannerView(context: ThemedReactContext ){
    when {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED -> {
        scannerView = ScannerView(context) { barcodes -> onScannedCallback(barcodes) }
        scannerView.setLayoutParams(
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        )
        rootView.addView(scannerView);
        rootView.setBackgroundColor( Color.BLACK)
      }
    }

  }

  @ReactProp(name = "cameraType")
  fun setCameraType(view: View, cameraType: String?) {
    if (scannerView.cameraType != cameraType ) {
        updateScannerProps(scannerView, "cameraType");
    }
    scannerView.cameraType = cameraType?:"back"
  }

  @ReactProp(name = "flashEnabled")
  fun setFlashEnabled(view: View, flashEnabled: Boolean) {
    if (scannerView.flashEnabled != flashEnabled ) {
        updateScannerProps(scannerView, "flashEnabled");
    }
    scannerView.flashEnabled = flashEnabled
  }

  @ReactProp(name = "scanEnabled")
  fun setScanEnabled(view: View, scanEnabled: Boolean) {
    if (scannerView.scanEnabled != scanEnabled ) {
        updateScannerProps(scannerView, "scanEnabled");
    }
    scannerView.scanEnabled = scanEnabled
  }

  override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> {
    return mapOf(
      "onScanned" to mapOf(
        "phasedRegistrationNames" to mapOf(
          "bubbled" to "onScanned"
        )
      )
    )
  }
}
