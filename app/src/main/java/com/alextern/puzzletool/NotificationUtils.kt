package com.alextern.puzzletool

import android.app.NotificationManager
import android.annotation.TargetApi
import android.app.Notification
import android.os.Build
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.util.Pair

object NotificationUtils {
    private const val NOTIFICATION_ID = 1337
    private const val NOTIFICATION_CHANNEL_ID = "com.alextern.puzzletool.app"
    private const val NOTIFICATION_CHANNEL_NAME = "com.alextern.puzzletool.app"
    @JvmStatic
    fun getNotification(context: Context): Pair<Int, Notification> {
        createNotificationChannel(context)
        val notification = createNotification(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        return Pair(NOTIFICATION_ID, notification)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(context: Context): Notification {
        val exitIntent = Intent(context, ToolsService::class.java).apply {
            action = ToolsService.kExitAction
        }
        val exitPendingIntent = PendingIntent.getService(context, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE)

        val showIntent = Intent(context, ToolsService::class.java).apply {
            action = ToolsService.kShowAction
        }
        val showPendingIntent = PendingIntent.getService(context, 0, showIntent, PendingIntent.FLAG_IMMUTABLE)

        val hideIntent = Intent(context, ToolsService::class.java).apply {
            action = ToolsService.kHideAction
        }
        val hidePendingIntent = PendingIntent.getService(context, 0, hideIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_search_black)
        builder.setContentTitle(context.getString(R.string.app_name))
        builder.setOngoing(true)
        builder.setCategory(Notification.CATEGORY_SERVICE)
        builder.setShowWhen(true)
        builder.addAction(R.drawable.ic_close, "Exit", exitPendingIntent)
        builder.addAction(R.drawable.ic_close, "Show", showPendingIntent)
        builder.addAction(R.drawable.ic_close, "Hide", hidePendingIntent)

        return builder.build()
    }
}