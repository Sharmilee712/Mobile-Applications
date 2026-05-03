package com.example.habitsnap.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.example.habitsnap.data.database.HabitSnapDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val db = HabitSnapDatabase.getDatabase(context)
            val habits = db.habitDao().getHabitsForUserSync(uid)
            habits.filter { it.reminderTime != null }.forEach { habit ->
                ReminderManager.scheduleReminder(
                    context, habit.id, habit.name, habit.reminderTime!!
                )
            }
        }
    }
}