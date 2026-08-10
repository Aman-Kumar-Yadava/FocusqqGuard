package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protected_apps")
data class ProtectedAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val dailyLimitMinutes: Int = 120, // Default 2 hours
    val continuousLimitMinutes: Int = 45, // Default 45 mins
    val allowedStartHour: Int = 7, // 07:00
    val allowedStartMinute: Int = 0,
    val allowedEndHour: Int = 22, // 22:30
    val allowedEndMinute: Int = 30,
    val isEnabled: Boolean = true,
    val isScheduleEnabled: Boolean = true
)
