package com.mvdown

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mvdown.databinding.ActivityProgressBinding
import com.mvdown.network.SseClient
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private lateinit var sseClient: SseClient
    private var downloadId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        
        downloadId = intent.getStringExtra("download_id")
        if (downloadId != null) {
            connectToProgress(downloadId!!)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Download Progress"
    }

    private fun connectToProgress(id: String) {
        sseClient = SseClient()
        
        lifecycleScope.launch {
            sseClient.connect(id) { progress ->
                runOnUiThread {
                    updateProgress(progress)
                }
            }
        }
    }

    private fun updateProgress(progress: Map<String, Any>) {
        val status = progress["status"] as? String ?: "unknown"
        val progressValue = (progress["progress"] as? Double)?.toInt() ?: 0
        val message = progress["message"] as? String ?: ""
        val speed = progress["speed"] as? String

        binding.progressBar.progress = progressValue
        binding.tvProgress.text = "$progressValue%"
        binding.tvStatus.text = message

        if (speed != null) {
            binding.tvSpeed.text = speed
        }

        when (status) {
            "completed" -> {
                binding.progressBar.progress = 100
                val filename = progress["filename"] as? String
                showSnackbar("Download completed: $filename")
                binding.btnDone.isEnabled = true
            }
            "error" -> {
                val error = progress["error"] as? String ?: "Unknown error"
                showSnackbar("Error: $error")
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sseClient.isInitialized) {
            sseClient.disconnect()
        }
    }
}
