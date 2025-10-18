package com.mvdown

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mvdown.databinding.ActivityProgressBinding
import com.mvdown.network.ApiClient
import com.mvdown.network.SseClient
import kotlinx.coroutines.launch

class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private lateinit var sseClient: SseClient
    private lateinit var notificationManager: NotificationManager
    private var downloadId: String? = null
    private var downloadedFileName: String? = null
    private var downloadUrl: String? = null
    private var isActivityVisible = true
    private var lastProgress = 0
    private var lastStatus = ""

    companion object {
        private const val CHANNEL_ID = "mvdown_progress"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        createNotificationChannel()
        requestNotificationPermission()
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        downloadId = intent.getStringExtra("download_id")
        if (downloadId != null) {
            connectToProgress(downloadId!!)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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

            lastProgress = progressValue
            lastStatus = message

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
                    binding.tvSpeed.text = "$speed • ETA: ${eta}s"
                }
            } else {
                binding.tvSpeed.visibility = View.GONE
            }

            when (status) {
                "completed" -> {
                    binding.progressBar.progress = 100
                    binding.tvProgress.text = "100%"
                    binding.tvStatus.text = "Processing completed!"
                    
                    downloadedFileName = progress["filename"] as? String
                    downloadUrl = progress["download_url"] as? String
                    
                    // Show download button
                    binding.btnDownloadFile.visibility = View.VISIBLE
                    binding.btnDownloadFile.isEnabled = true
                    binding.btnDone.isEnabled = true
                    
                    // Show completion notification
                    showCompletionNotification(downloadedFileName)
                    
                    showSnackbar("✅ Processing completed: $downloadedFileName")
                }
                "error" -> {
                    val error = progress["error"] as? String ?: "Unknown error"
                    binding.tvStatus.text = "Error: $error"
                    binding.tvSpeed.visibility = View.GONE
                    binding.btnDone.isEnabled = true
                    
                    // Show error notification
                    showErrorNotification(error)
                    
                    showSnackbar("❌ Error: $error")
                }
                "downloading" -> {
                    // Update notification when activity is not visible
                    if (!isActivityVisible) {
                        showProgressNotification(progressValue, message, speed)
                    }
                }
                "processing" -> {
                    binding.tvStatus.text = message
                    binding.tvSpeed.visibility = View.GONE
                    
                    // Update notification when activity is not visible
                    if (!isActivityVisible) {
                        showProgressNotification(progressValue, message, null)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Error parsing progress: ${e.message}")
        }
    }

    private fun showProgressNotification(progress: Int, status: String, speed: String?) {
        val intent = Intent(this, ProgressActivity::class.java).apply {
            putExtra("download_id", downloadId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (speed != null) {
            "$status • $speed"
        } else {
            status
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Processing Media ($progress%)")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(fileName: String?) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Processing Complete")
            .setContentText(fileName ?: "Media file ready")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(error: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Processing Failed")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
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

            showSnackbar("📥 Download started: $fileName")
            
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

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        // Cancel notification when activity is visible
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
        // Show notification with current progress when activity goes to background
        if (lastProgress < 100 && lastStatus.isNotEmpty()) {
            showProgressNotification(lastProgress, lastStatus, null)
        }
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
        // Clean up notification if download is complete
        if (lastProgress >= 100) {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}