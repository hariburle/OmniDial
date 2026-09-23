package com.example.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

object RoleHelper {

    fun isDefaultDialer(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == context.packageName
        }
    }

    fun createDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun isCallRedirectionRoleHeld(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            try {
                roleManager?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
            } catch (e: Exception) {
                android.util.Log.e("RoleHelper", "Failed to check ROLE_CALL_REDIRECTION", e)
                false
            }
        } else {
            false
        }
    }

    fun isCallRedirectionRoleAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            try {
                roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) == true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun createCallRedirectionRoleIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            try {
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
                } else {
                    createDefaultAppsSettingsIntent(context)
                }
            } catch (e: Exception) {
                createDefaultAppsSettingsIntent(context)
            }
        } else {
            createDefaultAppsSettingsIntent(context)
        }
    }

    fun createCallScreeningRoleIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            try {
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                } else {
                    createDefaultAppsSettingsIntent(context)
                }
            } catch (e: Exception) {
                createDefaultAppsSettingsIntent(context)
            }
        } else {
            createDefaultAppsSettingsIntent(context)
        }
    }

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            try {
                roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
            } catch (e: Exception) {
                android.util.Log.e("RoleHelper", "Failed to check ROLE_CALL_SCREENING", e)
                false
            }
        } else {
            false
        }
    }

    fun isCallScreeningRoleAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            try {
                roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun createDefaultAppsSettingsIntent(context: Context): Intent {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
        }
    }
}
