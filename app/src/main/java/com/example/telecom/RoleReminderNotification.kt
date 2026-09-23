package com.example.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

/**
 * Reminds the user to grant OmniDial the Call Redirection role at the moment
 * it matters most: when a car or watch with calling capability connects.
 * Without the role, calls started from the car/watch go through as plain
 * cellular and OmniDial can neither route nor warn about them.
 *
 * Shown at most once every 24 hours so it never nags on every drive.
 * Tapping it opens the Android system role-grant dialog directly.
 */
object RoleReminderNotification {

    const val CHANNEL_ID = "call_redirect_reminder_channel"
    const val NOTIFICATION_ID = 9002

    private const val PREFS = "kishan_dialer_prefs"
    private const val KEY_LAST_REMINDER_MS = "last_role_reminder_ms"
    private const val REMINDER_COOLDOWN_MS = 24 * 60 * 60 * 1000L

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call routing reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to enable call routing when your car or watch connects"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows the reminder unless the role is already held, the role isn't
     * available on this device, notifications are disabled, or we already
     * reminded within the last 24 hours.
     *
     * @param deviceLabel user-facing label, e.g. "car" or "watch"
     */
    fun maybeNotifyRoleMissing(context: Context, deviceLabel: String) {
        if (!RoleHelper.isCallRedirectionRoleAvailable(context)) return
        if (RoleHelper.isCallRedirectionRoleHeld(context)) {
            cancel(context)
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_REMINDER_MS, 0L) < REMINDER_COOLDOWN_MS) return

        val roleIntent = RoleHelper.createCallRedirectionRoleIntent(context) ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            roleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Your $deviceLabel connected — enable call routing?")
            .setContentText(
                "Calls from your $deviceLabel will go as plain cellular. " +
                    "Tap to let OmniDial route them through WhatsApp."
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your $deviceLabel just connected, but OmniDial is not your call " +
                        "redirection app, so calls started from the $deviceLabel will go " +
                        "through as plain cellular — WhatsApp rules won't apply and " +
                        "roaming charges can surprise you. Tap to enable routing."
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        prefs.edit().putLong(KEY_LAST_REMINDER_MS, now).apply()
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
