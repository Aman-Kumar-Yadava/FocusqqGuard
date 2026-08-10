package com.example.data.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.example.receiver.FocusGuardDeviceAdminReceiver

class DevicePolicyManagerWrapper(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    val adminComponent = ComponentName(context, FocusGuardDeviceAdminReceiver::class.java)

    /**
     * Checks if the app is provisioned as Device Owner.
     */
    fun isDeviceOwner(): Boolean {
        return dpm?.isDeviceOwnerApp(context.packageName) == true
    }

    /**
     * Checks if the Device Admin Receiver is active.
     */
    fun isAdminActive(): Boolean {
        return dpm?.isAdminActive(adminComponent) == true
    }

    /**
     * ADB command string to provision Device Owner in development mode.
     */
    fun getAdbCommand(): String {
        return "adb shell dpm set-device-owner ${context.packageName}/${adminComponent.className}"
    }

    /**
     * List of supported policies depending on Device Owner status.
     */
    fun getSupportedPolicies(): List<String> {
        return if (isDeviceOwner()) {
            listOf(
                "App Uninstall Restrictions (DISALLOW_UNINSTALL_APPS)",
                "System Settings Lock (DISALLOW_CONFIG_SETTINGS)",
                "System User Restriction Management",
                "Advanced Time Lock Policy"
            )
        } else {
            listOf(
                "Basic Usage Stats Tracking",
                "Accessibility Service Foreground Detection",
                "Overlay Alert Display",
                "Local Guardian PIN Lock"
            )
        }
    }
}
