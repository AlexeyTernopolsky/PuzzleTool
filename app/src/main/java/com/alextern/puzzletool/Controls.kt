package com.alextern.puzzletool

import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.coroutines.*


class Controls(private val service: ToolsService) {
    // declaring required variables
    private val mView: View
    private var mParams: WindowManager.LayoutParams? = null
    private val mWindowManager: WindowManager
    private val statusText: TextView
    private val modeText: TextView
    private val pointContainer: View
    private val pointImage: ImageView
    private val mainHandler = Handler(Looper.getMainLooper())

    var status: String = ""
        set(value) {
            mainHandler.post {
                statusText.text = value
            }
            field = value
        }
    var mode = ConverterType.kPuzzleDuel

    fun open() {
        mainHandler.post {
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
    }

    fun showPuzzleAction(pos: Pair<Int, Int>, action: Action) {
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                val y = pos.second - 730 - 200
                val x = pos.first + 10
                val margins = pointImage.layoutParams as RelativeLayout.LayoutParams
                margins.leftMargin = x
                margins.topMargin = y
                pointImage.layoutParams = margins
                val imageResId = when (action) {
                    Action.moveRight -> R.drawable.action_right
                    Action.moveDown -> R.drawable.action_down
                    Action.tap -> R.drawable.action_tap
                    Action.notFound -> 0
                }
                pointImage.setImageResource(imageResId)

                for (i in 0..3) {
                    pointContainer.visibility = View.VISIBLE
                    delay(200)
                    pointContainer.visibility = View.GONE
                    delay(200)
                }
            }
        }
    }

    private fun close() {
        mainHandler.post {
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
            680,
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

        modeText = mView.findViewById(R.id.button_mode)
        modeText.setOnClickListener { changeMode() }


        statusText = mView.findViewById(R.id.text_status)
        pointContainer = mView.findViewById(R.id.container_point)
        pointImage = mView.findViewById(R.id.image_point)

        // Define the position of the
        // window within the screen
        //mParams!!.gravity = Gravity.CENTER
        mWindowManager = service.getSystemService(WindowManager::class.java)
    }

    private fun changeMode() {
        when (mode) {
            ConverterType.kNormal -> {
                mode = ConverterType.kPuzzleDuel
                modeText.text = "D"
            }
            ConverterType.kPuzzleDuel -> {
                mode = ConverterType.kMasterPuzzle
                modeText.text = "M"
            }
            ConverterType.kMasterPuzzle -> {
                mode = ConverterType.kNormal
                modeText.text = "N"
            }
        }
    }
}