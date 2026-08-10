package com.example.data.manager

import android.content.Context

class DevicePolicyManagerWrapper(context: Context) {
    val controller = DevicePolicyController(context)

    fun isDeviceOwner(): Boolean = controller.isDeviceOwner()
    fun isAdminActive(): Boolean = controller.isAdminActive()
    fun getAdbCommand(): String = controller.getAdbCommand()
    fun getSupportedPolicies(): List<String> = controller.getSupportedPolicies()
    fun removeDeviceOwner(): Boolean = controller.removeDeviceOwner()

    fun setAppInstallationBlocked(blocked: Boolean): Boolean = controller.setAppInstallationBlocked(blocked)
    fun setAppUninstallationBlocked(blocked: Boolean): Boolean = controller.setAppUninstallationBlocked(blocked)
    fun setUninstallBlockedForPackage(packageName: String, blocked: Boolean): Boolean = controller.setUninstallBlockedForPackage(packageName, blocked)
}
