package com.example.habitsnap.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.habitsnap.data.dao.HabitDao
import com.example.habitsnap.data.dao.HabitLogDao
import com.example.habitsnap.data.entities.Habit
import com.example.habitsnap.data.entities.HabitLog

@Database(
    entities = [Habit::class, HabitLog::class],
    version = 1,
    exportSchema = false
)
abstract class HabitSnapDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao

    companion object {
        @Volatile
        private var INSTANCE: HabitSnapDatabase? = null

        fun getDatabase(context: Context): HabitSnapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitSnapDatabase::class.java,
                    "habitsnap_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}