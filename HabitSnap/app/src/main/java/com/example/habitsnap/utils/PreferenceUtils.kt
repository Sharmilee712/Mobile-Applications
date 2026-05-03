package com.example.habitsnap.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtils {

    private const val PREFS_NAME = "habitsnap_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setUserId(context: Context, uid: String) =
        prefs(context).edit().putString("user_id", uid).apply()

    fun getUserId(context: Context): String? =
        prefs(context).getString("user_id", null)

    fun clearUser(context: Context) =
        prefs(context).edit().clear().apply()

    fun setOnboardingDone(context: Context) =
        prefs(context).edit().putBoolean("onboarding_done", true).apply()

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean("onboarding_done", false)
}