package com.example.telecom

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.CallAttributes
import android.telecom.CallControl
import android.telecom.CallControlCallback
import android.telecom.CallEndpoint
import android.telecom.CallEventCallback
import android.telecom.CallException
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors
import java.util.function.Consumer

/**
 * Android 14+ (API 34+) Telecom VoIP Integration Helper.
 * Bridges external VoIP sessions (WhatsApp, WebRTC, SIP) into Android system-level audio routing,
 * device endpoints (Bluetooth/Car/Wearables), and native call waiting/hold management.
 */
object TelecomVoipHelper {
    private const val TAG = "TelecomVoipHelper"
    private val executor = Executors.newSingleThreadExecutor()

    private var activeCallControl: Any? = null
    private var isVoipActive = false

    /**
     * Registers and adds an inbound or outbound VoIP call using Android 14 Telecom addCall().
     * On pre-Android 14 devices, gracefully falls back to local CallManager orchestration.
     */
    fun startVoipCall(
        context: Context,
        phoneNumber: String,
        displayName: String,
        isIncoming: Boolean,
        onConnected: () -> Unit = {},
        onDisconnected: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val added = addCallApi34(
                    context = context,
                    phoneNumber = phoneNumber,
                    displayName = displayName,
                    isIncoming = isIncoming,
                    onConnected = onConnected,
                    onDisconnected = onDisconnected
                )
                if (added) return
            } catch (e: Exception) {
                Log.w(TAG, "Android 14 addCall failed or unsupported on this OEM device (${Build.MANUFACTURER}), falling back to legacy VoIP orchestration", e)
            }
        }

        // Fallback for Android 13 and below or OEM-restricted environments
        isVoipActive = true
        onConnected()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun addCallApi34(
        context: Context,
        phoneNumber: String,
        displayName: String,
        isIncoming: Boolean,
        onConnected: () -> Unit,
        onDisconnected: () -> Unit
    ): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return false

        val accountHandle = getVoipPhoneAccountHandle(context, telecomManager) ?: return false
        val addressUri = Uri.fromParts("tel", phoneNumber, null)
        val direction = if (isIncoming) CallAttributes.DIRECTION_INCOMING else CallAttributes.DIRECTION_OUTGOING

        val callAttributes = CallAttributes.Builder(accountHandle, direction, displayName, addressUri)
            .setCallType(CallAttributes.AUDIO_CALL)
            .setCallCapabilities(CallAttributes.SUPPORTS_SET_INACTIVE or CallAttributes.SUPPORTS_STREAM)
            .build()

        val callControlCallback = object : CallControlCallback {
            override fun onSetActive(consumer: Consumer<Boolean>) {
                Log.d(TAG, "onSetActive triggered by Telecom")
                consumer.accept(true)
            }

            override fun onSetInactive(consumer: Consumer<Boolean>) {
                Log.d(TAG, "onSetInactive (Hold) triggered by Telecom")
                consumer.accept(true)
            }

            override fun onAnswer(videoState: Int, consumer: Consumer<Boolean>) {
                Log.d(TAG, "onAnswer triggered by Telecom")
                consumer.accept(true)
            }

            override fun onDisconnect(disconnectCause: DisconnectCause, consumer: Consumer<Boolean>) {
                Log.d(TAG, "onDisconnect triggered by Telecom: ${disconnectCause.reason}")
                consumer.accept(true)
                activeCallControl = null
                isVoipActive = false
                onDisconnected()
            }

            override fun onCallStreamingStarted(consumer: Consumer<Boolean>) {
                Log.d(TAG, "onCallStreamingStarted triggered")
                consumer.accept(true)
            }
        }

        val callEventCallback = object : CallEventCallback {
            override fun onCallEndpointChanged(endpoint: CallEndpoint) {
                Log.d(TAG, "VoIP CallEndpoint changed to: ${endpoint.endpointName} (${endpoint.endpointType})")
            }

            override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
                Log.d(TAG, "Available VoIP CallEndpoints count: ${endpoints.size}")
            }

            override fun onMuteStateChanged(isMuted: Boolean) {
                Log.d(TAG, "VoIP Mute state changed to: $isMuted")
            }

            override fun onCallStreamingFailed(reason: Int) {
                Log.w(TAG, "VoIP Call streaming failed: $reason")
            }

            override fun onEvent(event: String, extras: android.os.Bundle) {
                Log.d(TAG, "VoIP Telecom custom event: $event")
            }
        }

        telecomManager.addCall(
            callAttributes,
            executor,
            object : OutcomeReceiver<CallControl, CallException> {
                override fun onResult(control: CallControl) {
                    Log.i(TAG, "VoIP CallControl successfully registered with Android 14 Telecom")
                    activeCallControl = control
                    isVoipActive = true

                    control.setActive(executor, object : OutcomeReceiver<Void, CallException> {
                        override fun onResult(result: Void?) {
                            Log.d(TAG, "VoIP call marked ACTIVE in Telecom subsystem")
                            onConnected()
                        }

                        override fun onError(error: CallException) {
                            Log.e(TAG, "Failed to set VoIP call active", error)
                        }
                    })
                }

                override fun onError(error: CallException) {
                    Log.e(TAG, "Failed to addCall in Android 14 Telecom", error)
                    isVoipActive = true
                    onConnected()
                }
            },
            callControlCallback,
            callEventCallback
        )
        return true
    }

    /**
     * Obtains or registers a Self-Managed PhoneAccountHandle for VoIP endpoints.
     * Validates OEM skins (Samsung OneUI, Xiaomi MIUI, Transsion, Huawei) where registering
     * self-managed phone accounts may throw SecurityExceptions or be blocked.
     */
    private fun getVoipPhoneAccountHandle(context: Context, telecomManager: TelecomManager): PhoneAccountHandle? {
        val componentName = ComponentName(context, TelecomCallService::class.java)
        val handle = PhoneAccountHandle(componentName, "OmniDialVoipEndpoint")

        try {
            val existing = telecomManager.getPhoneAccount(handle)
            if (existing == null || !existing.isEnabled) {
                val phoneAccount = PhoneAccount.builder(handle, "OmniDial VoIP")
                    .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED or PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS)
                    .setHighlightColor(0xFF2563EB.toInt())
                    .setShortDescription("OmniDial High-Definition Audio Endpoint")
                    .build()
                telecomManager.registerPhoneAccount(phoneAccount)
            }
            return handle
        } catch (se: SecurityException) {
            Log.w(TAG, "OEM security restriction (${Build.MANUFACTURER}) prevented self-managed PhoneAccount registration", se)
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Could not register or verify self-managed PhoneAccount for VoIP", e)
            return null
        }
    }

    /**
     * Ends the active VoIP call session in Android 14 Telecom subsystem.
     */
    fun endVoipCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val control = activeCallControl as? CallControl
            if (control != null) {
                try {
                    control.disconnect(
                        DisconnectCause(DisconnectCause.LOCAL, "User ended call"),
                        executor,
                        object : OutcomeReceiver<Void, CallException> {
                            override fun onResult(result: Void?) {
                                Log.d(TAG, "VoIP call disconnected from Telecom")
                            }

                            override fun onError(error: CallException) {
                                Log.e(TAG, "Failed to disconnect VoIP call", error)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error disconnecting CallControl", e)
                }
            }
        }
        activeCallControl = null
        isVoipActive = false
    }

    fun isVoipCallActive(): Boolean = isVoipActive
}
