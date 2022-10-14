package com.alextern.puzzletool

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.*
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

    var modeIndex = 0
        set(newValue) {
            field = newValue
            mainHandler.post {
                modeText.text = "M${newValue}"
                if (newValue in 1..4) {
                    val modeRadioIds = listOf(R.id.radio_mode1, R.id.radio_mode2, R.id.radio_mode3, R.id.radio_mode4)
                    val radioButton: RadioButton = mView.findViewById(modeRadioIds[newValue - 1])
                    radioButton.isChecked = true
                }
            }
        }
    var settingsSelection = false
    var modeSelection = false
    var colors = mutableSetOf(PuzzleColor.red, PuzzleColor.green, PuzzleColor.blue, PuzzleColor.yellow, PuzzleColor.violet)
    var captureEnabled = true

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

    fun close(hideOnly: Boolean = false) {
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
                if (hideOnly)
                    service.clearControls()
                else
                    service.stopWork()

            } catch (e: Exception) {
                Log.d("Error2", e.toString())
            }
        }
    }

    private fun startWork() {
        if (captureEnabled) {
            closeExtraUI()
            service.catchAndAnalyze()
        }
    }

    private fun captureScreen() {
        if (captureEnabled) {
            closeExtraUI()
            service.catchAndAnalyze(true)
        }
    }

    private fun closeExtraUI() {
        if (settingsSelection)
            openCloseSettingsView()
        if (modeSelection)
            openCloseModesView()
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

        var button: View = mView.findViewById(R.id.button_start)
        button.setOnClickListener { startWork() }

        button = mView.findViewById(R.id.button_capture)
        button.setOnClickListener { captureScreen() }

        button = mView.findViewById(R.id.button_exit)
        button.setOnClickListener {  close() }

        button = mView.findViewById(R.id.button_hide)
        button.setOnClickListener { close(hideOnly = true) }

        button = mView.findViewById(R.id.button_settings)
        button.setOnClickListener { openCloseSettingsView() }

        button = mView.findViewById(R.id.container_button_mode)
        button.setOnClickListener { openCloseModesView() }

        modeText = mView.findViewById(R.id.button_mode)

        val colorCheckboxIds = listOf(R.id.checkbox_color_red, R.id.checkbox_color_green, R.id.checkbox_color_blue, R.id.checkbox_color_yellow, R.id.checkbox_color_violet)
        val colorTypes = listOf(PuzzleColor.red, PuzzleColor.green, PuzzleColor.blue, PuzzleColor.yellow, PuzzleColor.violet)
        for (index in colorCheckboxIds.indices) {
            val colorCheckbox: CheckBox = mView.findViewById(colorCheckboxIds[index])
            colorCheckbox.setOnCheckedChangeListener { _, isChecked ->
                updateColors(isChecked, colorTypes[index])
            }
        }

        val modeRadioIds = listOf(R.id.radio_mode1, R.id.radio_mode2, R.id.radio_mode3, R.id.radio_mode4)
        for (index in modeRadioIds.indices) {
            val radio: RadioButton = mView.findViewById(modeRadioIds[index])
            radio.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    modeIndex = index + 1
                }
            }
        }

        statusText = mView.findViewById(R.id.text_status)
        pointContainer = mView.findViewById(R.id.container_point)
        pointImage = mView.findViewById(R.id.image_point)

        // Define the position of the
        // window within the screen
        //mParams!!.gravity = Gravity.CENTER
        mWindowManager = service.getSystemService(WindowManager::class.java)
    }

    private fun updateColors(checked: Boolean, color: PuzzleColor) {
        if (checked)
            colors.add(color)
        else
            colors.remove(color)
    }

    private fun openCloseSettingsView() {
        settingsSelection = !settingsSelection
        mView.findViewById<View>(R.id.container_settings).visibility = if (settingsSelection) View.VISIBLE else View.GONE
        mView.findViewById<View>(R.id.button_settings).setBackgroundColor(if (settingsSelection) Color.BLACK else Color.TRANSPARENT)
    }

    private fun openCloseModesView() {
        modeSelection = !modeSelection
        mView.findViewById<View>(R.id.container_modes).visibility = if (modeSelection) View.VISIBLE else View.GONE
        mView.findViewById<View>(R.id.container_button_mode).setBackgroundColor(if (modeSelection) Color.BLACK else Color.TRANSPARENT)
    }
}