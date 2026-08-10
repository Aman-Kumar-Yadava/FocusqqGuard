package com.example.data.repository

import com.example.data.db.dao.FocusGuardDao
import com.example.data.db.entities.AppSettingsEntity
import com.example.data.db.entities.DailyUsageEntity
import com.example.data.db.entities.FocusSessionEntity
import com.example.data.db.entities.GamingSessionEntity
import com.example.data.db.entities.ProtectedAppEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusGuardRepository(private val dao: FocusGuardDao) {

    val protectedApps: Flow<List<ProtectedAppEntity>> = dao.getAllProtectedApps()
    val appSettingsFlow: Flow<AppSettingsEntity?> = dao.getAppSettingsFlow()

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    suspend fun getProtectedApp(packageName: String): ProtectedAppEntity? {
        return dao.getProtectedApp(packageName)
    }

    suspend fun saveProtectedApp(app: ProtectedAppEntity) {
        dao.insertOrUpdateProtectedApp(app)
    }

    suspend fun deleteProtectedApp(packageName: String) {
        dao.deleteProtectedApp(packageName)
    }

    fun getDailyUsageForDate(dateString: String): Flow<List<DailyUsageEntity>> {
        return dao.getDailyUsageForDate(dateString)
    }

    fun getAllDailyUsageHistory(): Flow<List<DailyUsageEntity>> {
        return dao.getAllDailyUsageHistory()
    }

    suspend fun getDailyUsage(dateString: String, packageName: String): DailyUsageEntity? {
        return dao.getDailyUsage(dateString, packageName)
    }

    suspend fun updateDailyUsage(usage: DailyUsageEntity) {
        dao.insertOrUpdateDailyUsage(usage)
    }

    suspend fun recordGamingSession(session: GamingSessionEntity) {
        dao.insertGamingSession(session)
    }

    fun getGamingSessionsForDate(dateString: String): Flow<List<GamingSessionEntity>> {
        return dao.getGamingSessionsForDate(dateString)
    }

    suspend fun addFocusSession(session: FocusSessionEntity) {
        dao.insertFocusSession(session)
    }

    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>> {
        return dao.getAllFocusSessions()
    }

    fun getTodayEarnedGamingMinutes(dateString: String): Flow<Int?> {
        return dao.getEarnedGamingMinutesForDateFlow(dateString)
    }

    suspend fun getAppSettings(): AppSettingsEntity {
        return dao.getAppSettings() ?: AppSettingsEntity().also {
            dao.insertOrUpdateAppSettings(it)
        }
    }

    suspend fun updateAppSettings(settings: AppSettingsEntity) {
        dao.insertOrUpdateAppSettings(settings)
    }

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val settings = getAppSettings()
        if (!settings.isPinSet) return true // No PIN required if not set
        return settings.guardianPinHash == hashPin(pin)
    }

    suspend fun setGuardianPin(pin: String) {
        val settings = getAppSettings()
        val updated = settings.copy(
            guardianPinHash = hashPin(pin),
            isPinSet = true
        )
        dao.insertOrUpdateAppSettings(updated)
    }
}
