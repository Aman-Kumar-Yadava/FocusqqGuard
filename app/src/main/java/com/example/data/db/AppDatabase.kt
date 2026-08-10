package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.dao.FocusGuardDao
import com.example.data.db.entities.AppSettingsEntity
import com.example.data.db.entities.DailyUsageEntity
import com.example.data.db.entities.FocusSessionEntity
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
        FocusSessionEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
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
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default protected app (Free Fire) and app settings
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.focusGuardDao().insertOrUpdateProtectedApp(
                                    ProtectedAppEntity(
                                        packageName = "com.dts.freefireth",
                                        displayName = "Free Fire",
                                        dailyLimitMinutes = 120,
                                        continuousLimitMinutes = 45,
                                        allowedStartHour = 7,
                                        allowedStartMinute = 0,
                                        allowedEndHour = 22,
                                        allowedEndMinute = 30,
                                        isEnabled = true,
                                        isScheduleEnabled = true
                                    )
                                )
                                database.focusGuardDao().insertOrUpdateAppSettings(
                                    AppSettingsEntity(id = 1)
                                )
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
