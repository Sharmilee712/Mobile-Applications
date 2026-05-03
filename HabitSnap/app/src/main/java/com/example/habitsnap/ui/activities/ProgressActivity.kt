package com.example.habitsnap.ui.activities

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.example.habitsnap.databinding.ActivityProgressBinding
import com.example.habitsnap.ui.viewmodels.ProgressViewModel

class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Progress Dashboard"

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModel.loadStats(uid)

        viewModel.stats.observe(this) { stats ->
            binding.tvTotalHabits.text   = "🎯 Total Habits: ${stats.totalHabits}"
            binding.tvLongestStreak.text = "🔥 Best Streak: ${stats.longestStreak} days"
            binding.tvAvgStreak.text     = "📈 Avg Streak: ${"%.1f".format(stats.avgStreak)} days"
            binding.tvConsistency.text   = "💯 Consistency: ${"%.0f".format(stats.consistencyScore)}%"
            setupBarChart(stats.weeklyCompletions, stats.dayLabels)
            setupPieChart(stats.streakPerHabit)
        }
    }

    private fun setupBarChart(completions: List<Float>, labels: List<String>) {
        val entries = completions.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val dataSet = BarDataSet(entries, "Completion %").apply {
            color = Color.parseColor("#C9B8FF")
            valueTextColor = Color.DKGRAY
            valueTextSize  = 10f
        }
        binding.barChart.apply {
            data = BarData(dataSet)
            xAxis.apply {
                position       = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity    = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
            }
            axisRight.isEnabled   = false
            description.isEnabled = false
            legend.isEnabled      = false
            setFitBars(true)
            animateY(800)
            invalidate()
        }
    }

    private fun setupPieChart(streakData: List<Pair<String, Int>>) {
        if (streakData.isEmpty()) return
        val palette = listOf(
            "#FFB3C1","#C9B8FF","#B5EAD7","#FFE4B5","#B5D8FF",
            "#FFDAC1","#E2F0CB","#D4A5A5","#9EC1CF","#F0E6EF"
        )
        val entries = streakData.map { (name, streak) ->
            PieEntry(streak.toFloat().coerceAtLeast(1f), name)
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = streakData.mapIndexed { i, _ ->
                Color.parseColor(palette[i % palette.size])
            }
            valueTextSize  = 11f
            valueTextColor = Color.DKGRAY
            sliceSpace     = 3f
        }
        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled   = false
            isDrawHoleEnabled       = true
            holeRadius              = 45f
            transparentCircleRadius = 50f
            centerText              = "Streaks"
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor("#6B4EFF"))
            animateY(1000)
            invalidate()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}