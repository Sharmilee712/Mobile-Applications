package com.example.habitsnap.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.example.habitsnap.data.database.HabitSnapDatabase
import com.example.habitsnap.data.entities.HabitLog
import com.example.habitsnap.databinding.ActivityCameraCheckInBinding
import com.example.habitsnap.ml.HabitImageVerifier
import com.example.habitsnap.utils.CameraUtils
import com.example.habitsnap.utils.ImageHashUtils
import com.example.habitsnap.utils.LocationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCheckInActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HABIT_ID       = "extra_habit_id"
        const val EXTRA_HABIT_NAME     = "extra_habit_name"
        const val EXTRA_HABIT_CATEGORY = "extra_habit_category"
        private const val REQUEST_CAMERA_PERMISSION = 200
        private const val MIN_CHECKIN_GAP_MS = 60_000L
    }

    private lateinit var binding: ActivityCameraCheckInBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var habitId: Long = -1L
    private var habitName: String = ""
    private var habitCategory: String = "other"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraCheckInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitId       = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        habitName     = intent.getStringExtra(EXTRA_HABIT_NAME) ?: ""
        habitCategory = intent.getStringExtra(EXTRA_HABIT_CATEGORY) ?: "other"

        binding.tvHabitName.text = "📸 Snap: $habitName"
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
            )
        }

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val file = CameraUtils.createPhotoFile(this, habitId)

        binding.btnCapture.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lifecycleScope.launch { processPhoto(file.absolutePath) }
                }
                override fun onError(exc: ImageCaptureException) {
                    binding.btnCapture.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Capture failed: ${exc.message}",
                        Snackbar.LENGTH_SHORT).show()
                }
            }
        )
    }

    private suspend fun processPhoto(filePath: String) {
        val db = HabitSnapDatabase.getDatabase(this)

        // Anti-cheat 1: minimum time gap
        val latest = db.habitLogDao().getLatestLog(habitId)
        if (latest != null) {
            val gap = System.currentTimeMillis() - latest.timestamp
            if (gap < MIN_CHECKIN_GAP_MS) {
                showError("Too soon! Wait ${(MIN_CHECKIN_GAP_MS - gap) / 1000}s before next check-in.")
                return
            }
        }

        // Anti-cheat 2: duplicate image hash
        val hash = ImageHashUtils.computeSHA256(filePath)
        if (hash != null && db.habitLogDao().getLogByImageHash(hash) != null) {
            showError("❌ Duplicate image detected. Please take a new live photo.")
            return
        }

        // Timestamp overlay
        CameraUtils.stampTimestampOnImage(filePath)

        // ML Kit verification
        val bitmap = BitmapFactory.decodeFile(filePath)
        if (bitmap == null) {
            showError("Failed to decode image.")
            return
        }

        val mlResult = try {
            HabitImageVerifier.verify(bitmap, habitCategory)
        } catch (e: Exception) {
            null
        }

        if (mlResult != null && !mlResult.isValid) {
            showError(mlResult.message)
            return
        }

        // Location check
        val habit = db.habitDao().getHabitById(habitId)
        var checkLat: Double? = null
        var checkLng: Double? = null

        if (habit?.isLocationRequired == true &&
            habit.latitude != null &&
            habit.longitude != null
        ) {
            try {
                val loc = LocationUtils.getCurrentLocation(this)
                if (loc != null) {
                    checkLat = loc.latitude
                    checkLng = loc.longitude
                    if (!LocationUtils.isWithinHabitRadius(loc, habit.latitude, habit.longitude)) {
                        runOnUiThread {
                            Snackbar.make(
                                binding.root,
                                "⚠️ You are not at the habit location.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Save log
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val log = HabitLog(
            habitId      = habitId,
            date         = today,
            completed    = true,
            photoPath    = filePath,
            imageHash    = hash,
            timestamp    = System.currentTimeMillis(),
            latitude     = checkLat,
            longitude    = checkLng,
            mlLabels     = mlResult?.labels?.toString(),
            mlConfidence = mlResult?.confidence
        )
        db.habitLogDao().insertLog(log)

        // Recalculate streak
        if (habit != null) {
            val repo = com.example.habitsnap.data.repository.HabitRepository(
                db.habitDao(), db.habitLogDao()
            )
            repo.recalculateStreak(habit)
        }

        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "🔥 Habit checked in! Great job!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showError(msg: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.btnCapture.isEnabled = true
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}