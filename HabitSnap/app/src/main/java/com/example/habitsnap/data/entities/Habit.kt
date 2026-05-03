package com.example.habitsnap.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val description: String,
    val category: String,
    val emoji: String,
    val startDate: Long,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val reminderTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLocationRequired: Boolean = false,
    val lastCompletedDate: Long? = null
)
