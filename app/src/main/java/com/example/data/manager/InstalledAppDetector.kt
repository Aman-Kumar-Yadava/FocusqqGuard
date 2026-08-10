package com.example.data.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class InstalledAppInfo(
    val displayName: String,
    val packageName: String,
    val isProtected: Boolean = false
)

class InstalledAppDetector(private val context: Context) {

    fun getInstalledNonSystemApps(protectedPackageNames: Set<String> = emptySet()): List<InstalledAppInfo> {
        val pm = context.packageManager
        val installedApps = mutableListOf<InstalledAppInfo>()

        return try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (!isSystemApp && appInfo.packageName != context.packageName) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    installedApps.add(
                        InstalledAppInfo(
                            displayName = if (label.isNotBlank()) label else appInfo.packageName,
                            packageName = appInfo.packageName,
                            isProtected = protectedPackageNames.contains(appInfo.packageName)
                        )
                    )
                }
            }
            installedApps.sortedBy { it.displayName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
