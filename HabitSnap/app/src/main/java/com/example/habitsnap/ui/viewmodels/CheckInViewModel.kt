package com.example.habitsnap.ui.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.habitsnap.data.database.HabitSnapDatabase
import com.example.habitsnap.data.entities.Habit
import com.example.habitsnap.data.entities.HabitLog
import com.example.habitsnap.data.repository.HabitRepository
import kotlinx.coroutines.launch

sealed class CheckInState {
    object Idle : CheckInState()
    object Loading : CheckInState()
    data class Success(val message: String) : CheckInState()
    data class Error(val message: String) : CheckInState()
    data class DuplicateImage(val message: String) : CheckInState()
    data class MLRejected(val message: String, val labels: List<String>) : CheckInState()
    data class LocationWarning(val message: String) : CheckInState()
}

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val db = HabitSnapDatabase.getDatabase(application)
    private val repo = HabitRepository(db.habitDao(), db.habitLogDao())

    private val _checkInState = MutableLiveData<CheckInState>(CheckInState.Idle)
    val checkInState: LiveData<CheckInState> = _checkInState

    private val _currentHabit = MutableLiveData<Habit?>()
    val currentHabit: LiveData<Habit?> = _currentHabit

    fun loadHabit(habitId: Long) = viewModelScope.launch {
        _currentHabit.postValue(repo.getHabitById(habitId))
    }

    fun setLoading() { _checkInState.value = CheckInState.Loading }
    fun setError(msg: String) { _checkInState.value = CheckInState.Error(msg) }
    fun setSuccess(msg: String) { _checkInState.value = CheckInState.Success(msg) }
    fun setMLRejected(msg: String, labels: List<String>) { _checkInState.value = CheckInState.MLRejected(msg, labels) }
    fun setDuplicate(msg: String) { _checkInState.value = CheckInState.DuplicateImage(msg) }
    fun resetState() { _checkInState.value = CheckInState.Idle }

    suspend fun isImageDuplicate(hash: String): Boolean =
        db.habitLogDao().getLogByImageHash(hash) != null

    suspend fun getLatestLog(habitId: Long): HabitLog? =
        db.habitLogDao().getLatestLog(habitId)

    suspend fun saveLog(log: HabitLog): Long =
        db.habitLogDao().insertLog(log)

    suspend fun recalculateStreak(habit: Habit) =
        repo.recalculateStreak(habit)
}