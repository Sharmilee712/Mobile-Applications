package com.example.habitsnap.notifications

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderManager {

    fun scheduleReminder(context: Context, habitId: Long, habitName: String, timeString: String) {
        val parts = timeString.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)

        val delay = target.timeInMillis - now.timeInMillis

        val data = workDataOf(
            ReminderWorker.KEY_HABIT_ID to habitId,
            ReminderWorker.KEY_HABIT_NAME to habitName
        )

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_$habitId")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reminder_$habitId",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelReminder(context: Context, habitId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$habitId")
    }
    fun scheduleImmediateTest(context: Context, habitId: Long, habitName: String) {
        try {
            val data = workDataOf(
                ReminderWorker.KEY_HABIT_ID to habitId,
                ReminderWorker.KEY_HABIT_NAME to habitName
            )

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(request)
            android.util.Log.d("ReminderManager", "Test notification scheduled in 5 seconds!")
        } catch (e: Exception) {
            android.util.Log.e("ReminderManager", "Error: ${e.message}")
        }
    }

}