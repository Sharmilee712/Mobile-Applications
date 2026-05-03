package com.example.habitsnap.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.example.habitsnap.data.database.HabitSnapDatabase
import com.example.habitsnap.data.entities.Habit
import com.example.habitsnap.databinding.ActivityAddEditHabitBinding
import com.example.habitsnap.notifications.ReminderManager
import kotlinx.coroutines.launch
import java.util.*

class AddEditHabitActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
        private val CATEGORIES = listOf(
            "workout", "reading", "coding", "meditation",
            "cooking", "walking", "sleep", "water", "other"
        )
        private val EMOJIS = mapOf(
            "workout" to "🏋️", "reading" to "📚", "coding" to "💻",
            "meditation" to "🧘", "cooking" to "🍳", "walking" to "🚶",
            "sleep" to "😴", "water" to "💧", "other" to "⭐"
        )
    }

    private lateinit var binding: ActivityAddEditHabitBinding
    private var editingHabitId: Long = -1L
    private var existingHabit: Habit? = null
    private var selectedReminderTime: String? = null
    private var savedLat: Double? = null
    private var savedLng: Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditHabitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        editingHabitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)

        val spinnerAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            CATEGORIES
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter

        if (editingHabitId != -1L) {
            supportActionBar?.title = "Edit Habit"
            loadHabit()
        } else {
            supportActionBar?.title = "New Habit"
        }

        binding.btnSetReminder.setOnClickListener { showTimePicker() }
        binding.btnSaveLocation.setOnClickListener { captureCurrentLocation() }
        binding.btnSave.setOnClickListener { saveHabit() }
    }

    private fun loadHabit() {
        lifecycleScope.launch {
            val db = HabitSnapDatabase.getDatabase(this@AddEditHabitActivity)
            existingHabit = db.habitDao().getHabitById(editingHabitId) ?: return@launch
            existingHabit?.let { h ->
                binding.etHabitName.setText(h.name)
                binding.etDescription.setText(h.description)
                val pos = CATEGORIES.indexOf(h.category).coerceAtLeast(0)
                binding.spinnerCategory.setSelection(pos)
                selectedReminderTime = h.reminderTime
                h.reminderTime?.let { binding.tvReminderSet.text = "⏰ Reminder: $it" }
                savedLat = h.latitude
                savedLng = h.longitude
                if (h.latitude != null) {
                    binding.tvLocationSet.text = "📍 Location saved"
                    binding.switchLocationRequired.isChecked = h.isLocationRequired
                }
            }
        }
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            selectedReminderTime = "%02d:%02d".format(hour, minute)
            binding.tvReminderSet.text = "⏰ Reminder: $selectedReminderTime"
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    @SuppressLint("MissingPermission")
    private fun captureCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                savedLat = loc.latitude
                savedLng = loc.longitude
                binding.tvLocationSet.text =
                    "📍 Location saved (%.4f, %.4f)".format(loc.latitude, loc.longitude)
            } else {
                Snackbar.make(binding.root, "Could not get location.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveHabit() {
        val name = binding.etHabitName.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val category = CATEGORIES[binding.spinnerCategory.selectedItemPosition]
        val emoji = EMOJIS[category] ?: "⭐"
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (name.isEmpty()) {
            binding.etHabitName.error = "Habit name required"
            return
        }

        val habit = if (existingHabit != null) {
            existingHabit!!.copy(
                name = name, description = desc,
                category = category, emoji = emoji,
                reminderTime = selectedReminderTime,
                latitude = savedLat, longitude = savedLng,
                isLocationRequired = binding.switchLocationRequired.isChecked
            )
        } else {
            Habit(
                userId = uid, name = name, description = desc,
                category = category, emoji = emoji,
                startDate = System.currentTimeMillis(),
                reminderTime = selectedReminderTime,
                latitude = savedLat, longitude = savedLng,
                isLocationRequired = binding.switchLocationRequired.isChecked
            )
        }

        lifecycleScope.launch {
            val db = HabitSnapDatabase.getDatabase(this@AddEditHabitActivity)
            val habitId = if (existingHabit != null) {
                db.habitDao().updateHabit(habit)
                habit.id
            } else {
                db.habitDao().insertHabit(habit)
            }

            selectedReminderTime?.let {
                ReminderManager.scheduleReminder(
                    this@AddEditHabitActivity, habitId, habit.name, it
                )
            }

            // Test notification in 5 seconds
            ReminderManager.scheduleImmediateTest(
                this@AddEditHabitActivity, habitId, habit.name
            )

            finish()
        }
    }


    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}