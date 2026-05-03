package com.example.habitsnap.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.habitsnap.data.entities.HabitLog
import com.example.habitsnap.databinding.ItemFeedBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FeedAdapter : ListAdapter<HabitLog, FeedAdapter.FeedViewHolder>(DiffCallback()) {

    inner class FeedViewHolder(private val binding: ItemFeedBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: HabitLog) {
            val ts = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(Date(log.timestamp))
            binding.tvTimestamp.text = "📅 $ts"
            binding.tvDate.text      = log.date

            log.photoPath?.let { path ->
                Glide.with(binding.ivProof)
                    .load(File(path))
                    .centerCrop()
                    .into(binding.ivProof)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FeedViewHolder(
            ItemFeedBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<HabitLog>() {
        override fun areItemsTheSame(old: HabitLog, new: HabitLog) = old.id == new.id
        override fun areContentsTheSame(old: HabitLog, new: HabitLog) = old == new
    }
}