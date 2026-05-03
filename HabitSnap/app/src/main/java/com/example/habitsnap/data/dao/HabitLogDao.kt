package com.example.habitsnap.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.habitsnap.data.entities.HabitLog

@Dao
interface HabitLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog): Long

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY timestamp DESC")
    fun getLogsForHabit(habitId: Long): LiveData<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY timestamp DESC")
    suspend fun getLogsForHabitSync(habitId: Long): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLogForDate(habitId: Long, date: String): HabitLog?

    @Query("SELECT * FROM habit_logs WHERE imageHash = :hash LIMIT 1")
    suspend fun getLogByImageHash(hash: String): HabitLog?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLog(habitId: Long): HabitLog?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date >= :fromDate ORDER BY date ASC")
    suspend fun getLogsFromDate(habitId: Long, fromDate: String): List<HabitLog>

    @Query("SELECT * FROM habit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<HabitLog>>

    @Query("SELECT * FROM habit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): LiveData<List<HabitLog>>

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Long)
}
