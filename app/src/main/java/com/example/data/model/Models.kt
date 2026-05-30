package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // Coding, Reading, Fitness, Health, Photography, Meditation, Learning, Custom
    val iconName: String, // Icon ID or string tag (e.g. "code", "book", "fitness", "health", "meditation", etc.)
    val colorHex: String, // Color hex string
    val startDate: Long,
    val endDate: Long?, // Optional end date
    val reminderTime: String?, // Format "HH:mm", null if no reminder
    val targetDays: Int, // Target days
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val active: Boolean = true // True for active, false if archived
) : Serializable

@Entity(tableName = "completion_records")
data class CompletionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val challengeId: Int,
    val date: String, // Format "YYYY-MM-DD"
    val completed: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
