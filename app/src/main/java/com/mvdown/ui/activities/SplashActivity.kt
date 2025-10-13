package com.mvdown.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.mvdown.MainActivity
import com.mvdown.R
import com.mvdown.api.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var animationView: LottieAnimationView
    private lateinit var tvStatus: TextView
    private lateinit var btnRetry: Button

    // Register a permission launcher (modern API)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkServerHealth()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to load downloads",
                    Toast.LENGTH_LONG
                ).show()
                btnRetry.visibility = View.VISIBLE
                tvStatus.text = "Permission denied"
                animationView.pauseAnimation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initViews()
        checkAndRequestPermission()
    }

    private fun initViews() {
        animationView = findViewById(R.id.animationView)
        tvStatus = findViewById(R.id.tvStatus)
        btnRetry = findViewById(R.id.btnRetry)

        btnRetry.setOnClickListener {
            btnRetry.visibility = View.GONE
            checkAndRequestPermission()
        }
    }

    // ✅ Ask for runtime permission here
    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_* instead of READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                checkServerHealth()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                checkServerHealth()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkServerHealth() {
        tvStatus.text = "Checking server status..."
        animationView.playAnimation()

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.healthCheck()

                if (response.isSuccessful) {
                    tvStatus.text = "Server is ready!"
                    delay(800)
                    proceedToMain()
                } else {
                    showError("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Connection failed: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        tvStatus.text = message
        btnRetry.visibility = View.VISIBLE
        animationView.pauseAnimation()
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left)
    }
}
