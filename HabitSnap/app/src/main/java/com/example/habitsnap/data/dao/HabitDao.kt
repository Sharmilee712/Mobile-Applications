package com.example.habitsnap.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.habitsnap.data.entities.Habit

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY startDate DESC")
    fun getHabitsForUser(userId: String): LiveData<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("UPDATE habits SET currentStreak = :streak, longestStreak = :longestStreak, lastCompletedDate = :date WHERE id = :habitId")
    suspend fun updateStreak(habitId: Long, streak: Int, longestStreak: Int, date: Long)

    @Query("SELECT * FROM habits WHERE userId = :userId")
    suspend fun getHabitsForUserSync(userId: String): List<Habit>
}
