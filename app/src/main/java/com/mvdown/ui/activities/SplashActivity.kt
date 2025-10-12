package com.mvdown.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initViews()
        checkServerHealth()
    }

    private fun initViews() {
        animationView = findViewById(R.id.animationView)
        tvStatus = findViewById(R.id.tvStatus)
        btnRetry = findViewById(R.id.btnRetry)

        btnRetry.setOnClickListener {
            btnRetry.visibility = View.GONE
            checkServerHealth()
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
                    // Add a small delay to show the success message
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