package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.dao.FocusGuardDao
import com.example.data.db.entities.AppSettingsEntity
import com.example.data.db.entities.DailyUsageEntity
import com.example.data.db.entities.GamingSessionEntity
import com.example.data.db.entities.ProtectedAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProtectedAppEntity::class,
        DailyUsageEntity::class,
        GamingSessionEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun focusGuardDao(): FocusGuardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusguard_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL(
                            "INSERT OR REPLACE INTO protected_apps " +
                            "(packageName, displayName, dailyLimitMinutes, continuousLimitMinutes, allowedStartHour, allowedStartMinute, allowedEndHour, allowedEndMinute, isEnabled, isScheduleEnabled) " +
                            "VALUES ('com.dts.freefireth', 'Free Fire', 120, 45, 7, 0, 22, 30, 1, 1)"
                        )
                        db.execSQL(
                            "INSERT OR REPLACE INTO app_settings " +
                            "(id, guardianPinHash, isPinSet, globalNightLockEnabled, nightLockStartHour, nightLockStartMinute, nightLockEndHour, nightLockEndMinute, warning30mSent, warning15mSent, warning5mSent, warning1mSent, isDebugSimulationEnabled, simulatedUsageSeconds) " +
                            "VALUES (1, '', 0, 1, 22, 30, 7, 0, 0, 0, 0, 0, 0, 0)"
                        )
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
