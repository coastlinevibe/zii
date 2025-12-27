package com.zii.school.activation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.zii.school.MainActivity
import com.zii.school.R

/**
 * Worker to show expiry notifications
 */
class ExpiryNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val CHANNEL_ID = "activation_expiry"
        private const val CHANNEL_NAME = "Subscription Expiry"
        private const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        val message = inputData.getString("message") ?: return Result.failure()
        val daysRemaining = inputData.getInt("daysRemaining", 0)

        showNotification(message, daysRemaining)

        return Result.success()
    }

    private fun showNotification(message: String, daysRemaining: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
            as NotificationManager

        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about subscription expiry"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle(if (daysRemaining > 0) "Subscription Expiring Soon" else "Subscription Expired")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (daysRemaining > 0) {
                    "$message. Renew now to continue using online features."
                } else {
                    "$message. Purchase a new activation code to continue using online features."
                }
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                0,
                if (daysRemaining > 0) "Renew Now" else "Buy Code",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
