package com.example.habitsnap.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.habitsnap.data.entities.Habit

class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
    override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean =
        oldItem == newItem
}