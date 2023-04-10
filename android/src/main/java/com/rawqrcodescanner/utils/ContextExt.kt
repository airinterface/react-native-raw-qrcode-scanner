package com.rawqrcodescanner.utils

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.facebook.react.bridge.ReactContext
import com.rawqrcodescanner.camera.ScannerView

val Context.displayRotation: Int
  get() {
    try{
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Context.display
        this.display?.let { display ->
          return display.rotation
        }

        // ReactContext.currentActivity.display
        if (this is ReactContext) {
          currentActivity?.display?.let { display ->
            return display.rotation
          }
        }
      }

      // WindowManager.defaultDisplay
      val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      if (windowManager != null) {
        @Suppress("DEPRECATION") // deprecated since SDK 30
        windowManager.defaultDisplay?.let { display ->
          return display.rotation
        }
      }
    } catch ( e: Exception ) {
      Log.e(ScannerView.TAG, e.toString() )
    }

    // 0
    return Surface.ROTATION_0
  }
