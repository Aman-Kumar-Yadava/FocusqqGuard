package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entities.AppSettingsEntity
import com.example.data.db.entities.DailyUsageEntity
import com.example.data.db.entities.FocusSessionEntity
import com.example.data.db.entities.GamingSessionEntity
import com.example.data.db.entities.ProtectedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusGuardDao {

    // Protected Apps
    @Query("SELECT * FROM protected_apps")
    fun getAllProtectedApps(): Flow<List<ProtectedAppEntity>>

    @Query("SELECT * FROM protected_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getProtectedApp(packageName: String): ProtectedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProtectedApp(app: ProtectedAppEntity)

    @Query("DELETE FROM protected_apps WHERE packageName = :packageName")
    suspend fun deleteProtectedApp(packageName: String)

    // Daily Usage
    @Query("SELECT * FROM daily_usage WHERE dateString = :dateString AND packageName = :packageName LIMIT 1")
    suspend fun getDailyUsage(dateString: String, packageName: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE dateString = :dateString")
    fun getDailyUsageForDate(dateString: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY dateString DESC")
    fun getAllDailyUsageHistory(): Flow<List<DailyUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyUsage(usage: DailyUsageEntity)

    // Gaming Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamingSession(session: GamingSessionEntity)

    @Query("SELECT * FROM gaming_sessions WHERE dateString = :dateString ORDER BY startTime DESC")
    fun getGamingSessionsForDate(dateString: String): Flow<List<GamingSessionEntity>>

    // Focus Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE dateString = :dateString")
    fun getFocusSessionsForDate(dateString: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(earnedGamingMinutes) FROM focus_sessions WHERE dateString = :dateString AND isCompleted = 1")
    fun getEarnedGamingMinutesForDateFlow(dateString: String): Flow<Int?>

    @Query("SELECT SUM(earnedGamingMinutes) FROM focus_sessions WHERE dateString = :dateString AND isCompleted = 1")
    suspend fun getEarnedGamingMinutesForDate(dateString: String): Int?

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppSettings(settings: AppSettingsEntity)
}
