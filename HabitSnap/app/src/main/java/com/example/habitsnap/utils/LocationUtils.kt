package com.example.habitsnap.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object LocationUtils {

    private const val ACCEPTABLE_RADIUS_METERS = 200.0

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? =
        suspendCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    fun isWithinHabitRadius(
        current: Location,
        habitLat: Double,
        habitLng: Double
    ): Boolean {
        val target = Location("habit").apply {
            latitude = habitLat
            longitude = habitLng
        }
        return current.distanceTo(target) <= ACCEPTABLE_RADIUS_METERS
    }

    fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
}