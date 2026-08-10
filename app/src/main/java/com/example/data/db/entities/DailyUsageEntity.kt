package com.example.data.db.entities

import androidx.room.Entity

@Entity(
    tableName = "daily_usage",
    primaryKeys = ["dateString", "packageName"]
)
data class DailyUsageEntity(
    val dateString: String, // Format: YYYY-MM-DD
    val packageName: String,
    val totalUsedSeconds: Long = 0L,
    val sessionCount: Int = 0,
    val longestSessionSeconds: Long = 0L
)
