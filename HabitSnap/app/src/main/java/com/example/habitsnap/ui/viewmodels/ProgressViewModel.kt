package com.example.habitsnap.ui.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.habitsnap.data.database.HabitSnapDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProgressStats(
    val totalHabits: Int,
    val longestStreak: Int,
    val avgStreak: Float,
    val consistencyScore: Float,
    val weeklyCompletions: List<Float>,
    val dayLabels: List<String>,
    val streakPerHabit: List<Pair<String, Int>>
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HabitSnapDatabase.getDatabase(application)
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _stats = MutableLiveData<ProgressStats>()
    val stats: LiveData<ProgressStats> = _stats

    fun loadStats(userId: String) = viewModelScope.launch {
        val habits = db.habitDao().getHabitsForUserSync(userId)
        if (habits.isEmpty()) {
            _stats.postValue(ProgressStats(0, 0, 0f, 0f,
                List(7) { 0f },
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun"),
                emptyList()))
            return@launch
        }

        val totalHabits   = habits.size
        val longestStreak = habits.maxOf { it.longestStreak }
        val avgStreak     = habits.map { it.currentStreak }.average().toFloat()

        val dayLabels   = mutableListOf<String>()
        val completions = mutableListOf<Float>()
        val dayFmt      = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = sdf.format(cal.time)
            dayLabels.add(dayFmt.format(cal.time))
            var done = 0
            habits.forEach { habit ->
                val log = db.habitLogDao().getLogForDate(habit.id, dateStr)
                if (log?.completed == true) done++
            }
            completions.add(if (totalHabits > 0) done.toFloat() / totalHabits * 100f else 0f)
        }

        val consistencyScore = completions.average().toFloat()
        val streakPerHabit = habits.map { Pair(it.name, it.currentStreak) }

        _stats.postValue(ProgressStats(
            totalHabits, longestStreak, avgStreak,
            consistencyScore, completions, dayLabels, streakPerHabit
        ))
    }
}