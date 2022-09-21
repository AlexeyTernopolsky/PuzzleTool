package com.alextern.puzzletool

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView

const val kStartWorkMode = 0
const val kAskDrawPermissionMode = 1
const val kAskCapturePermissionMode = 2

const val kModeKey = "mode"

class ToolsActivity : Activity() {
    private var mode = kStartWorkMode

    /****************************************** Activity Lifecycle methods  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mode = savedInstanceState.getInt(kModeKey, kStartWorkMode)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(kModeKey, mode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startService(ToolsService.getStartIntent(this, resultCode, data))
                mode = kStartWorkMode
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        when (mode) {
            kStartWorkMode, kAskDrawPermissionMode -> startProjection()
        }
    }

    private fun startProjection() {
        if (checkOverlayPermission()) {
            val mProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mode = kAskCapturePermissionMode
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        }
    }

    // method to ask user to grant the Overlay permission
    private fun checkOverlayPermission():Boolean {
        if (!Settings.canDrawOverlays(this)) {
            when (mode) {
                kStartWorkMode -> {
                    // send user to the device settings
                    val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivity(myIntent)
                    mode = kAskDrawPermissionMode
                }
                kAskDrawPermissionMode -> {
                    // the user cancel to provide the permissions, simple exit from app
                    mode = kStartWorkMode
                    finish()
                }
            }
            return false
        }
        return true
    }

    companion object {
        private const val REQUEST_CODE = 100
    }
}