package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gaming_sessions")
data class GamingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val dateString: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long
)
