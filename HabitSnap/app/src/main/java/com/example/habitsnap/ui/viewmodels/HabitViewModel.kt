package com.example.habitsnap.ui.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.habitsnap.data.database.HabitSnapDatabase
import com.example.habitsnap.data.entities.Habit
import com.example.habitsnap.data.entities.HabitLog
import com.example.habitsnap.data.repository.HabitRepository
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HabitSnapDatabase.getDatabase(application)
    private val repo = HabitRepository(db.habitDao(), db.habitLogDao())

    private val _userId = MutableLiveData<String>()

    val habits: LiveData<List<Habit>> = _userId.switchMap { uid ->
        repo.getHabitsForUser(uid)
    }

    private val _operationResult = MutableLiveData<Result<String>>()
    val operationResult: LiveData<Result<String>> = _operationResult

    fun setUser(userId: String) { _userId.value = userId }

    fun addHabit(habit: Habit) = viewModelScope.launch {
        try {
            repo.insertHabit(habit)
            _operationResult.postValue(Result.success("Habit added!"))
        } catch (e: Exception) {
            _operationResult.postValue(Result.failure(e))
        }
    }

    fun updateHabit(habit: Habit) = viewModelScope.launch {
        try {
            repo.updateHabit(habit)
            _operationResult.postValue(Result.success("Habit updated!"))
        } catch (e: Exception) {
            _operationResult.postValue(Result.failure(e))
        }
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        try {
            repo.deleteHabit(habit)
            _operationResult.postValue(Result.success("Habit deleted!"))
        } catch (e: Exception) {
            _operationResult.postValue(Result.failure(e))
        }
    }

    fun getLogsForHabit(habitId: Long) = repo.getLogsForHabit(habitId)

    suspend fun getLogForToday(habitId: Long): HabitLog? = repo.getLogForToday(habitId)

    fun recalculateStreak(habit: Habit) = viewModelScope.launch {
        repo.recalculateStreak(habit)
    }

    fun getRecentLogs() = repo.getRecentLogs()
}