package com.example.data.manager

import android.content.Context

class OverlayManager(context: Context) {
    val manager = GamingBlockOverlayManager(context)

    fun hasOverlayPermission(): Boolean = manager.hasOverlayPermission()
    fun showBlockingScreen(packageName: String, reasonDescription: String) = manager.showBlockingScreen(packageName, reasonDescription)
    fun getOverlaySettingsIntent() = manager.getOverlaySettingsIntent()
}
