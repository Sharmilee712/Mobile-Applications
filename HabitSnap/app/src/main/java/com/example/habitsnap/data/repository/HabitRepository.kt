package com.example.habitsnap.data.repository

import androidx.lifecycle.LiveData
import com.example.habitsnap.data.dao.HabitDao
import com.example.habitsnap.data.dao.HabitLogDao
import com.example.habitsnap.data.entities.Habit
import com.example.habitsnap.data.entities.HabitLog
import java.text.SimpleDateFormat
import java.util.*

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getHabitsForUser(userId: String): LiveData<List<Habit>> =
        habitDao.getHabitsForUser(userId)

    suspend fun getHabitById(habitId: Long): Habit? =
        habitDao.getHabitById(habitId)

    suspend fun insertHabit(habit: Habit): Long =
        habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) =
        habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) {
        habitLogDao.deleteLogsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    fun getLogsForHabit(habitId: Long): LiveData<List<HabitLog>> =
        habitLogDao.getLogsForHabit(habitId)

    suspend fun insertLog(log: HabitLog): Long =
        habitLogDao.insertLog(log)

    suspend fun getLogForToday(habitId: Long): HabitLog? {
        val today = dateFormat.format(Date())
        return habitLogDao.getLogForDate(habitId, today)
    }

    suspend fun isImageHashDuplicate(hash: String): Boolean =
        habitLogDao.getLogByImageHash(hash) != null

    suspend fun getLatestLog(habitId: Long): HabitLog? =
        habitLogDao.getLatestLog(habitId)

    fun getRecentLogs(): LiveData<List<HabitLog>> =
        habitLogDao.getRecentLogs()

    suspend fun recalculateStreak(habit: Habit): Pair<Int, Int> {
        val logs = habitLogDao.getLogsForHabitSync(habit.id)
            .filter { it.completed }
            .sortedByDescending { it.date }

        if (logs.isEmpty()) return Pair(0, habit.longestStreak)

        val cal = Calendar.getInstance()
        val today = dateFormat.format(cal.time)
        val yesterday = run {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d = dateFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            d
        }

        val mostRecentDate = logs.first().date
        if (mostRecentDate != today && mostRecentDate != yesterday) {
            return Pair(0, habit.longestStreak)
        }

        var streak = 0
        var checkDate = if (mostRecentDate == today) today else yesterday
        val calCheck = Calendar.getInstance()

        for (log in logs) {
            if (log.date == checkDate) {
                streak++
                calCheck.time = dateFormat.parse(checkDate)!!
                calCheck.add(Calendar.DAY_OF_YEAR, -1)
                checkDate = dateFormat.format(calCheck.time)
            } else {
                break
            }
        }

        val longest = maxOf(streak, habit.longestStreak)
        habitDao.updateStreak(habit.id, streak, longest, System.currentTimeMillis())
        return Pair(streak, longest)
    }
}