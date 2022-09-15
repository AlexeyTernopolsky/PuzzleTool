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


class ToolsActivity : Activity() {
    var checkDrawPermissions: Boolean = false

    /****************************************** Activity Lifecycle methods  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // start projection
        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener { startProjection() }

        findViewById<Button>(R.id.testButton).setOnClickListener { startControlsTest() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startService(ToolsService.getStartIntent(this, resultCode, data))
                finish()
                /*val background = findViewById<ImageView>(R.id.image_background)
                background.visibility = View.VISIBLE
                background.setImageResource(R.drawable.test1)*/
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkDrawPermissions) {
            checkDrawPermissions = false
            startProjection()
        }
    }

    private fun startProjection() {
        if (checkOverlayPermission()) {
            val mProjectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        }
    }

    private fun startControlsTest() {
        startService(ToolsService.getTestControlIntent(this))
        moveTaskToBack(true)
    }

    // method to ask user to grant the Overlay permission
    private fun checkOverlayPermission():Boolean {
        if (!Settings.canDrawOverlays(this)) {
            // send user to the device settings
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(myIntent)
            checkDrawPermissions = true
            return false
        }
        return true
    }

    companion object {
        private const val REQUEST_CODE = 100
    }
}