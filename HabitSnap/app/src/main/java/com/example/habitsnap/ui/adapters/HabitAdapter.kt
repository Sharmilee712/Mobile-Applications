package com.example.habitsnap.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.habitsnap.data.entities.Habit
import com.example.habitsnap.databinding.ItemHabitBinding
import java.text.SimpleDateFormat
import java.util.*

class HabitAdapter(
    private val onCheckIn: (Habit) -> Unit,
    private val onEdit: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(DiffCallback()) {

    inner class HabitViewHolder(private val binding: ItemHabitBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: Habit) {
            binding.tvEmoji.text     = habit.emoji
            binding.tvHabitName.text = habit.name
            binding.tvCategory.text  = habit.category.replaceFirstChar { it.uppercase() }
            binding.tvStreak.text    = if (habit.currentStreak > 0)
                "🔥 ${habit.currentStreak} day streak" else "Start your streak!"

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val doneToday = habit.lastCompletedDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(java.util.Date(it)) == today
            } ?: false

            binding.btnCheckIn.text      = if (doneToday) "✅ Done" else "📸 Check In"
            binding.btnCheckIn.isEnabled = !doneToday
            binding.cardView.alpha       = if (doneToday) 0.85f else 1f

            binding.btnCheckIn.setOnClickListener { onCheckIn(habit) }
            binding.btnEdit.setOnClickListener    { onEdit(habit) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        HabitViewHolder(
            ItemHabitBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(old: Habit, new: Habit) = old.id == new.id
        override fun areContentsTheSame(old: Habit, new: Habit) = old == new
    }
}