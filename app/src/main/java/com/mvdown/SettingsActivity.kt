package com.mvdown

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mvdown.databinding.ActivitySettingsBinding
import com.mvdown.util.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }

    private fun loadSettings() {
        binding.etApiUrl.setText(preferenceManager.getApiUrl())
        
        val theme = preferenceManager.getTheme()
        binding.tvThemeValue.text = theme.capitalize()
    }

    private fun setupListeners() {
        binding.btnSaveApi.setOnClickListener {
            val url = binding.etApiUrl.text.toString().trim()
            preferenceManager.saveApiUrl(url)
            showSnackbar("API URL saved")
        }

        binding.cardTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("System Default", "Light", "Dark")
        val currentTheme = preferenceManager.getTheme()
        val selectedIndex = when (currentTheme) {
            "light" -> 1
            "dark" -> 2
            else -> 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val newTheme = when (which) {
                    1 -> "light"
                    2 -> "dark"
                    else -> "system"
                }
                
                preferenceManager.saveTheme(newTheme)
                applyTheme(newTheme)
                binding.tvThemeValue.text = themes[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun applyTheme(theme: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("M/V Down")
            .setMessage("Version: 1.0\n\nA powerful media downloader for YouTube, Spotify and more.\n\nDeveloped with ❤️")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSnackbar(message: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}