package com.example.habitsnap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.example.habitsnap.R
import com.example.habitsnap.databinding.ActivityMainBinding
import com.example.habitsnap.ui.adapters.HabitAdapter
import com.example.habitsnap.ui.viewmodels.HabitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HabitViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        auth = FirebaseAuth.getInstance()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            redirectToLogin()
            return
        }

        val displayName = auth.currentUser?.displayName ?: "Snapper"
        binding.tvGreeting.text = "Hey $displayName 👋"

        setupRecyclerView()
        setupNavigation()

        // Set user AFTER setup to avoid ANR
        viewModel.setUser(uid)

        viewModel.habits.observe(this) { habits ->
            adapter.submitList(habits)
            binding.tvEmptyState.visibility =
                if (habits.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = HabitAdapter(
            onCheckIn = { habit ->
                startActivity(
                    Intent(this, CameraCheckInActivity::class.java)
                        .putExtra(CameraCheckInActivity.EXTRA_HABIT_ID, habit.id)
                        .putExtra(CameraCheckInActivity.EXTRA_HABIT_NAME, habit.name)
                        .putExtra(CameraCheckInActivity.EXTRA_HABIT_CATEGORY, habit.category)
                )
            },
            onEdit = { habit ->
                startActivity(
                    Intent(this, AddEditHabitActivity::class.java)
                        .putExtra(AddEditHabitActivity.EXTRA_HABIT_ID, habit.id)
                )
            }
        )
        binding.rvHabits.layoutManager = LinearLayoutManager(this)
        binding.rvHabits.adapter = adapter
    }

    private fun setupNavigation() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddEditHabitActivity::class.java))
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_progress -> {
                    startActivity(Intent(this, ProgressActivity::class.java))
                    true
                }
                R.id.nav_feed -> {
                    startActivity(Intent(this, SocialFeedActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                redirectToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun redirectToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }
}