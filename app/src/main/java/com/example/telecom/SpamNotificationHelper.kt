package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object SpamNotificationHelper {
    const val CHANNEL_ID = "blocked_spam_channel_v1"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Blocked Spam Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a suspected spam call is automatically blocked"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showBlockedSpamNotification(context: Context, number: String, callerName: String) {
        createNotificationChannel(context)

        val notificationId = (number.hashCode() and 0x7FFFFFFF)

        // Open app directly to Recents (Call Log)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_INITIAL_TAB", 1)
            putExtra("EXTRA_NAV_TAB", "RECENTS")
            putExtra("EXTRA_NAV_TAB_INDEX", 1)
            putExtra("EXTRA_HIGHLIGHT_NUMBER", number)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Call Back Action
        val callBackIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Not Spam / Whitelist Action
        val unspamIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_UNSPAM
            putExtra(CallNotificationReceiver.EXTRA_PHONE_NUMBER, number)
            putExtra(CallNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val unspamPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            unspamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayName = if (callerName.isNotBlank() && callerName != number) callerName else "Suspected Spam"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Blocked Spam Call: $displayName")
            .setContentText("$number was blocked by Carrier Spam Filter. Tap to review.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Incoming call from $displayName ($number) was blocked by Carrier Spam Filter. If this was misclassified, you can return the call or mark as Not Spam.")
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .addAction(android.R.drawable.ic_menu_call, "Return Call", callBackPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Not Spam", unspamPendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }
}
