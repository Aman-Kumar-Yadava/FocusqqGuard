package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subject: String,
    val durationMinutes: Int,
    val earnedGamingMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val isCompleted: Boolean = true
)
