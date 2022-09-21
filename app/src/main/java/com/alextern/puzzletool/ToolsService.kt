package com.alextern.puzzletool

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.WindowManager
import com.alextern.puzzletool.NotificationUtils.getNotification

class ToolsService : Service() {
    private var mMediaProjection: MediaProjection? = null
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private var projCode = 0
    private var projData: Intent? = null
    private var controls: Controls? = null

    private inner class ImageAvailableListener : OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            mImageReader!!.acquireLatestImage().use { image ->
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * mWidth

                    // create bitmap
                    val bitmap = Bitmap.createBitmap(
                        mWidth + rowPadding / pixelStride,
                        mHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    analyzeBitmap(bitmap)
                    freeVirtualDisplay()
                }
            }
        }
    }

    private fun analyzeBitmap(bitmap: Bitmap) {
        if (catchOnly) {
            controls?.status = "Captured"
            saveImageInQ(bitmap)
        } else {
            val converter = BitmapToPuzzleConverter(bitmap, controls?.mode ?: ConverterType.kPuzzleDuel)
            val puzzle = converter.analyze()
            if (puzzle.isValid()) {
                val optimizer = PuzzleOptimizer(puzzle)
                optimizer.optimize()
                val pos = converter.cellCoordinate(optimizer.actionX, optimizer.actionY)
                controls?.status = "${optimizer.maxPoints}"
                controls?.showPuzzleAction(pos, optimizer.actionType)
            } else if (tryCount < 5) {
                tryCount += 1
                mHandler?.postDelayed({
                    freeVirtualDisplay()
                    createVirtualDisplay()
                }, 200)
            } else {
                print(puzzle.toString())
                controls?.status = "Failed"
            }
        }
    }

    private fun saveImageInQ(bitmap: Bitmap): Uri? {
        val filename = "tools_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = application.contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            val fos = contentResolver.openOutputStream(imageUri)



            fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(imageUri, contentValues, null, null)
        }

        return imageUri
    }

    private inner class OrientationChangeCallback internal constructor(context: Context?) :
        OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = mDisplay!!.rotation
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay!!.release()
                    if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.e(TAG, "stopping projection.")
            mHandler!!.post {
                freeVirtualDisplay()
                mMediaProjection!!.unregisterCallback(this@MediaProjectionStopCallback)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when {
            isStartCommand(intent) -> {
                // create notification
                val notification = getNotification(this)
                startForeground(notification.first, notification.second)
                // store data and show interface
                projCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
                projData = intent.getParcelableExtra(DATA)

                showInterface()
                startProjection(projCode, projData)
            }
            intent.action == kExitAction -> {
                val ctl = controls
                if (ctl != null) {
                    ctl.close()
                } else {
                    stopWork()
                }
            }
            intent.action == kShowAction -> {
                if (controls == null) {
                    showInterface()
                }
            }
            intent.action == kHideAction -> {
                if (controls != null) {
                    controls?.close(true)
                    controls = null
                }
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    var tryCount = 0
    var catchOnly = false

    fun catchAndAnalyze(catchOnly: Boolean = false) {
        // create virtual display depending on device width / height
        tryCount = 0
        this.catchOnly = catchOnly
        controls?.status = "..."
        freeVirtualDisplay()
        createVirtualDisplay()
    }

    fun stopWork() {
        stopProjection()
        stopSelf()
    }

    private fun freeVirtualDisplay() {
        if (mVirtualDisplay != null) mVirtualDisplay!!.release()
        if (mImageReader != null) mImageReader!!.setOnImageAvailableListener(null, null)
    }

    private fun showInterface() {
        controls = Controls(this)
        controls?.open()
    }

    private fun startProjection(resultCode: Int, data: Intent?) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mMediaProjection == null) {
            mMediaProjection = mpManager.getMediaProjection(resultCode, data!!)
            if (mMediaProjection != null) {
                // display metrics
                mDensity = Resources.getSystem().displayMetrics.densityDpi
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                mDisplay = windowManager.defaultDisplay
                val screenResolution = Point()
                mDisplay?.getRealSize(screenResolution)
                mWidth = screenResolution.x
                mHeight = screenResolution.y

                // register media projection stop callback
                mMediaProjection!!.registerCallback(MediaProjectionStopCallback(), mHandler)
            }
        }
    }

    private fun stopProjection() {
        if (mHandler != null) {
            mHandler?.post {
                if (mMediaProjection != null) {
                    mMediaProjection!!.stop()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            SCREENCAP_NAME, mWidth, mHeight,
            mDensity, virtualDisplayFlags, mImageReader!!.surface, null, mHandler
        )
        mImageReader!!.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    companion object {
        const val kExitAction = "Action.Exit"
        const val kShowAction = "Action.Show"
        const val kHideAction = "Action.Hide"
        private const val TAG = "ScreenCaptureService"
        private const val RESULT_CODE = "RESULT_CODE"
        private const val DATA = "DATA"
        private const val ACTION = "ACTION"
        private const val START = "START"
        private const val TEST_CONTROLS = "TEST_CONTROLS"
        private const val SCREENCAP_NAME = "screencap"
        fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
            val intent = Intent(context, ToolsService::class.java)
            intent.putExtra(ACTION, START)
            intent.putExtra(RESULT_CODE, resultCode)
            intent.putExtra(DATA, data)
            return intent
        }

        private fun isStartCommand(intent: Intent): Boolean {
            return (intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                    && intent.hasExtra(ACTION) && intent.getStringExtra(ACTION) == START)
        }

        private val virtualDisplayFlags: Int
            get() = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
}