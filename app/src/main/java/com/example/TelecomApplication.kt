package com.example

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.example.telecom.CarBluetoothReceiver
import com.example.util.ContactPhotoFetcher

class TelecomApplication : Application(), ImageLoaderFactory {
    private val carBluetoothReceiver = CarBluetoothReceiver()

    override fun onCreate() {
        super.onCreate()
        // ACTION_ACL_CONNECTED is not delivered to manifest-declared receivers
        // on API 26+, so register at runtime while the process is alive.
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(carBluetoothReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(carBluetoothReceiver, filter)
        }
    }
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(ContactPhotoFetcher.Factory(this@TelecomApplication))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
