package com.mvdown.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "MVDownPrefs",
        Context.MODE_PRIVATE
    )

    fun saveApiUrl(url: String) {
        prefs.edit().putString(KEY_API_URL, url).apply()
    }

    fun getApiUrl(): String {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(): String {
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }

    companion object {
        private const val KEY_API_URL = "api_url"
        private const val KEY_THEME = "theme"
        private const val DEFAULT_API_URL = "http://192.168.1.100:8000"
    }
}
