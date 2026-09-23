package com.example.telecom

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Stamps the time a car/infotainment Bluetooth system connects. The dialer's
 * [com.example.ui.components.CallRedirectionBanner] reads the stamp and shows
 * car-specific copy while it's fresh — no notification, no extra permission.
 *
 * Also acts as the safety check you asked for: whenever a car or a watch with
 * calling capability connects, it verifies OmniDial still holds the Call
 * Redirection role ("default call forwarding app"). If the role is missing,
 * it posts a reminder notification (at most once per 24h) whose tap opens the
 * system role-grant dialog directly.
 *
 * Registered at runtime from TelecomApplication.onCreate(): ACTION_ACL_CONNECTED
 * is not delivered to manifest-declared receivers on API 26+, so a manifest
 * entry alone never fires on modern Android. (The manifest entry is kept for
 * API 24-25; delivery there is harmless and idempotent.)
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    companion object {
        const val PREFS = "kishan_dialer_prefs"
        const val KEY_LAST_CAR_CONNECT_MS = "last_car_bt_connect_ms"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device: BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        device ?: return

        val deviceClass = try {
            device.bluetoothClass?.deviceClass
        } catch (_: SecurityException) {
            // BLUETOOTH_CONNECT not granted on Android 12+ — can't verify, stay quiet.
            null
        } ?: return

        val isCarInfotainment = deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
        val isWatch = deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH
        if (!isCarInfotainment && !isWatch) return

        if (isCarInfotainment) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_CAR_CONNECT_MS, System.currentTimeMillis())
                .apply()
        }

        // Safety check: is OmniDial still the default call forwarding app?
        // If the role is missing, calls from this car/watch go as plain cellular.
        RoleReminderNotification.maybeNotifyRoleMissing(
            context,
            deviceLabel = if (isWatch) "watch" else "car"
        )
    }
}
