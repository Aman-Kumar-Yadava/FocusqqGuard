package com.example.data.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.example.receiver.FocusGuardDeviceAdminReceiver

enum class DeviceOwnerState {
    DEVICE_OWNER_ACTIVE,
    DEVICE_OWNER_INACTIVE
}

enum class PolicySupportState {
    POLICY_SUPPORTED,
    POLICY_UNSUPPORTED
}

class DevicePolicyController(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    val adminComponent = ComponentName(context, FocusGuardDeviceAdminReceiver::class.java)

    fun getDeviceOwnerState(): DeviceOwnerState {
        return if (isDeviceOwner()) DeviceOwnerState.DEVICE_OWNER_ACTIVE else DeviceOwnerState.DEVICE_OWNER_INACTIVE
    }

    fun isDeviceOwner(): Boolean {
        return try {
            dpm?.isDeviceOwnerApp(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    fun isAdminActive(): Boolean {
        return try {
            dpm?.isAdminActive(adminComponent) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Attempts to suspend or unsuspend a package using official DevicePolicyManager APIs.
     * Returns true if successfully executed.
     */
    fun setPackageSuspended(packageName: String, suspended: Boolean): Boolean {
        if (!isDeviceOwner() || dpm == null) return false
        return try {
            val result = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), suspended)
            result.isEmpty() // empty array means no errors suspending
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Prevents or allows installation of new applications on the device.
     */
    fun setAppInstallationBlocked(blocked: Boolean): Boolean {
        if (!isDeviceOwner() || dpm == null) return false
        return try {
            if (blocked) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Prevents or allows uninstallation of apps.
     */
    fun setAppUninstallationBlocked(blocked: Boolean): Boolean {
        if (!isDeviceOwner() || dpm == null) return false
        return try {
            if (blocked) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Blocks uninstallation for a specific protected package.
     */
    fun setUninstallBlockedForPackage(packageName: String, blocked: Boolean): Boolean {
        if (!isDeviceOwner() || dpm == null) return false
        return try {
            dpm.setUninstallBlocked(adminComponent, packageName, blocked)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears Device Owner status for FocusGuard.
     */
    fun removeDeviceOwner(): Boolean {
        if (!isDeviceOwner() || dpm == null) return false
        return try {
            // First clear user restrictions and uninstall blocks
            setAppInstallationBlocked(false)
            setAppUninstallationBlocked(false)
            dpm.clearDeviceOwnerApp(context.packageName)
            !isDeviceOwner()
        } catch (e: Exception) {
            false
        }
    }

    fun getAdbCommand(): String {
        return "adb shell dpm set-device-owner ${context.packageName}/${adminComponent.className}"
    }

    fun getSupportedPolicies(): List<String> {
        return if (isDeviceOwner()) {
            listOf(
                "Package Suspension & Application Blocking (setPackagesSuspended)",
                "Prevent Installation of Apps (DISALLOW_INSTALL_APPS)",
                "Prevent Uninstallation of Protected Apps (DISALLOW_UNINSTALL_APPS & setUninstallBlocked)",
                "System Settings Protection (DISALLOW_CONFIG_SETTINGS)",
                "Advanced Time Lock Policy"
            )
        } else {
            listOf(
                "Usage Access Stats Monitoring (UsageStatsManager)",
                "Full-Screen Blocking Interface (BlockingActivity)",
                "Guardian PIN Configuration Lock"
            )
        }
    }
}
