package com.example.habitsnap.ui.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.habitsnap.databinding.ActivitySocialFeedBinding
import com.example.habitsnap.ui.adapters.FeedAdapter
import com.example.habitsnap.ui.viewmodels.SocialFeedViewModel

class SocialFeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocialFeedBinding
    private val viewModel: SocialFeedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Habit Feed 📸"

        val adapter = FeedAdapter()
        binding.rvFeed.layoutManager = LinearLayoutManager(this)
        binding.rvFeed.adapter = adapter

        viewModel.recentLogs.observe(this) { logs ->
            val filtered = logs.filter { it.completed && it.photoPath != null }
            adapter.submitList(filtered)
            binding.tvEmptyFeed.visibility =
                if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
