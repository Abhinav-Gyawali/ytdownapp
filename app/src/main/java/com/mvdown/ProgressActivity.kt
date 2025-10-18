package com.mvdown

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mvdown.databinding.ActivityProgressBinding
import com.mvdown.network.ApiClient
import com.mvdown.network.SseClient
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private lateinit var sseClient: SseClient
    private var downloadId: String? = null
    private var downloadedFileName: String? = null
    private var downloadUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        
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

    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            finish()
        }

        binding.btnDownloadFile.setOnClickListener {
            downloadedFileName?.let { fileName ->
                downloadFileToDevice(fileName)
            }
        }
    }

    private fun connectToProgress(id: String) {
        sseClient = SseClient()
        
        lifecycleScope.launch {
            try {
                sseClient.connect(id) { progress ->
                    runOnUiThread {
                        updateProgress(progress)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showSnackbar("Connection error: ${e.message}")
                }
            }
        }
    }

    private fun updateProgress(progress: Map<String, Any>) {
        try {
            val status = progress["status"] as? String ?: "unknown"
            
            // Handle progress value - can be String or Number
            val progressValue = when (val prog = progress["progress"]) {
                is Number -> prog.toInt()
                is String -> prog.replace("%", "").toIntOrNull() ?: 0
                else -> 0
            }
            
            val message = progress["message"] as? String ?: ""
            val speed = progress["speed"] as? String

            // Update UI
            binding.progressBar.progress = progressValue
            binding.progressBar.max = 100
            binding.tvProgress.text = "$progressValue%"
            binding.tvStatus.text = message

            if (speed != null && speed.isNotEmpty()) {
                binding.tvSpeed.visibility = View.VISIBLE
                binding.tvSpeed.text = speed
                
                val eta = progress["eta"]
                if (eta != null) {
                    binding.tvSpeed.text = "$speed â€¢ ETA: ${eta}s"
                }
            } else {
                binding.tvSpeed.visibility = View.GONE
            }

            when (status) {
                "completed" -> {
                    binding.progressBar.progress = 100
                    binding.tvProgress.text = "100%"
                    binding.tvStatus.text = "Download completed!"
                    
                    downloadedFileName = progress["filename"] as? String
                    downloadUrl = progress["download_url"] as? String
                    
                    // Show download button
                    binding.btnDownloadFile.visibility = View.VISIBLE
                    binding.btnDownloadFile.isEnabled = true
                    binding.btnDone.isEnabled = true
                    
                    showSnackbar("âœ… Download completed: $downloadedFileName")
                }
                "error" -> {
                    val error = progress["error"] as? String ?: "Unknown error"
                    binding.tvStatus.text = "Error: $error"
                    binding.tvSpeed.visibility = View.GONE
                    binding.btnDone.isEnabled = true
                    showSnackbar("âŒ Error: $error")
                }
                "downloading" -> {
                    // Progress is being downloaded
                }
                "processing" -> {
                    binding.tvStatus.text = message
                    binding.tvSpeed.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Error parsing progress: ${e.message}")
        }
    }

    private fun downloadFileToDevice(fileName: String) {
        try {
            val url = "${ApiClient.BASE_URL}/downloads/${Uri.encode(fileName)}"
            
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading from M/V Down")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            showSnackbar("ðŸ“¥ Download started: $fileName")
            
            // Optional: Navigate back after starting download
            binding.btnDone.postDelayed({
                finish()
            }, 2000)
            
        } catch (e: Exception) {
            showSnackbar("Error starting download: ${e.message}")
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
