package com.alextern.puzzletool

import android.graphics.PixelFormat
import android.util.Log
import android.view.*
import android.widget.TextView


class Controls(private val service: ToolsService) {
    // declaring required variables
    private val mView: View
    private var mParams: WindowManager.LayoutParams? = null
    private val mWindowManager: WindowManager
    private val statusText: TextView
    var status: String = ""
        set(value) {
            statusText.text = value
            field = value
        }

    fun open() {
        try {
            // check if the view is already
            // inflated or present in the window
            if (mView.windowToken == null) {
                if (mView.parent == null) {
                    mWindowManager.addView(mView, mParams)
                }
            }
        } catch (e: Exception) {
            Log.d("Error1", e.toString())
        }
    }

    private fun close() {
        try {
            // remove the view from the window
            mWindowManager.removeView(mView)
            // invalidate the view
            mView.invalidate()
            // remove all views
            (mView.parent as? ViewGroup)?.removeAllViews()

            // the above steps are necessary when you are adding and removing
            // the view simultaneously, it might give some exceptions
            service.stopWork()
        } catch (e: Exception) {
            Log.d("Error2", e.toString())
        }
    }

    private fun startWork() {
        service.catchAndStart()
    }

    init {
        // set the layout parameters of the window
        mParams = WindowManager.LayoutParams( // Shrink the window to wrap the content rather
            // than filling the screen
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,  // Display it on top of other application windows
            0,
            200,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Don't let it grab the input focus
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,  // Make the underlying application window visible
            // through any transparent parts
            PixelFormat.TRANSLUCENT
        )
        mParams?.gravity = Gravity.TOP
        // getting a LayoutInflater
        val layoutInflater = service.getSystemService(LayoutInflater::class.java)
        // inflating the view with the custom layout we created
        mView = layoutInflater.inflate(R.layout.overlay, null)
        // set onClickListener on the remove button, which removes
        // the view from the window
        var button: View = mView.findViewById(R.id.button_close)
        button.setOnClickListener { close() }

        button = mView.findViewById(R.id.button_start)
        button.setOnClickListener { startWork() }

        statusText = mView.findViewById(R.id.text_status)

        // Define the position of the
        // window within the screen
        //mParams!!.gravity = Gravity.CENTER
        mWindowManager = service.getSystemService(WindowManager::class.java)
    }
}