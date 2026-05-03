package com.example.habitsnap.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.habitsnap.data.database.HabitSnapDatabase

class SocialFeedViewModel(application: Application) : AndroidViewModel(application) {
    private val db = HabitSnapDatabase.getDatabase(application)
    val recentLogs = db.habitLogDao().getRecentLogs()
}