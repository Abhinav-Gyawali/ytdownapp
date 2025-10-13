package com.mvdown.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.mvdown.R
import com.mvdown.models.DownloadEvent

class DownloadProgressDialog(context: Context) : Dialog(context) {
    
    private lateinit var tvTitle: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvEta: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_download_progress_simple)
        setCancelable(false)
        
        initViews()
        tvTitle.text = "Processing your request..."
    }
    
    private fun initViews() {
        tvTitle = findViewById(R.id.tvDownloadTitle)
        tvProgress = findViewById(R.id.tvProgress)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvEta = findViewById(R.id.tvEta)
        progressBar = findViewById(R.id.downloadProgress)
    }
    
    fun updateProgress(event: DownloadEvent.Progress) {
        when (event.status) {
            "starting", "connected" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = event.message ?: "Initializing download..."
                tvSpeed.text = "Speed: --"
                tvEta.text = "ETA: --"
                progressBar.isIndeterminate = true
                progressBar.progress = 0
            }
            
            "downloading" -> {
                tvTitle.text = "Downloading..."
                progressBar.isIndeterminate = false
                
                // Update progress percentage
                event.percent?.let { percentStr ->
                    tvProgress.text = percentStr
                    
                    // Extract numeric value and update progress bar
                    val percentValue = percentStr.replace("%", "").trim().toDoubleOrNull() ?: 0.0
                    progressBar.progress = percentValue.toInt()
                }
                
                // Update speed
                event.speed?.let { speedInBytesPerSec ->
                    val speedMB = speedInBytesPerSec / (1024.0 * 1024.0)
                    tvSpeed.text = if (speedMB > 0) {
                        String.format("Speed: %.2f MB/s", speedMB)
                    } else {
                        "Speed: --"
                    }
                } ?: run {
                    tvSpeed.text = "Speed: --"
                }
                
                // Update ETA
                event.eta?.let { etaSeconds ->
                    tvEta.text = if (etaSeconds > 0) {
                        formatETA(etaSeconds)
                    } else {
                        "ETA: --"
                    }
                } ?: run {
                    tvEta.text = "ETA: --"
                }
                
                // Show downloaded/total if available
                if (event.downloadedBytes != null && event.totalBytes != null && event.totalBytes > 0) {
                    val downloadedMB = event.downloadedBytes / (1024.0 * 1024.0)
                    val totalMB = event.totalBytes / (1024.0 * 1024.0)
                    tvProgress.text = "${event.percent ?: "0%"} (${String.format("%.1f", downloadedMB)} / ${String.format("%.1f", totalMB)} MB)"
                }
            }
            
            "processing" -> {
                tvTitle.text = "Processing..."
                tvProgress.text = event.message ?: "Finalizing download..."
                tvSpeed.text = "Speed: --"
                tvEta.text = "ETA: --"
                progressBar.isIndeterminate = true
                progressBar.progress = 100
            }
            
            "zipping" -> {
                tvTitle.text = "Creating Archive..."
                tvProgress.text = event.message ?: "Compressing files..."
                tvSpeed.text = "Speed: --"
                tvEta.text = "ETA: --"
                progressBar.isIndeterminate = true
                progressBar.progress = 100
            }
            
            "closed" -> {
                tvTitle.text = "Connection Closed"
                tvProgress.text = event.message ?: "Connection closed"
                tvSpeed.text = "Speed: --"
                tvEta.text = "ETA: --"
                progressBar.isIndeterminate = false
                progressBar.progress = 0
            }
            
            else -> {
                // Handle unknown status
                tvTitle.text = "Processing..."
                tvProgress.text = event.message ?: event.status
                tvSpeed.text = "Speed: --"
                tvEta.text = "ETA: --"
                progressBar.isIndeterminate = true
            }
        }
    }
    
    /**
     * Format ETA in a human-readable format
     * @param seconds ETA in seconds
     * @return Formatted string like "ETA: 1m 30s" or "ETA: 45s"
     */
    private fun formatETA(seconds: Int): String {
        return when {
            seconds < 0 -> "ETA: --"
            seconds < 60 -> "ETA: ${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                "ETA: ${minutes}m ${secs}s"
            }
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "ETA: ${hours}h ${minutes}m"
            }
        }
    }
    
    /**
     * Show completion state
     */
    fun showComplete(filename: String) {
        tvTitle.text = "Download Complete!"
        tvProgress.text = filename
        tvSpeed.text = ""
        tvEta.text = ""
        progressBar.isIndeterminate = false
        progressBar.progress = 100
    }
    
    /**
     * Show error state
     */
    fun showError(error: String) {
        tvTitle.text = "Download Failed"
        tvProgress.text = error
        tvSpeed.text = ""
        tvEta.text = ""
        progressBar.isIndeterminate = false
        progressBar.progress = 0
    }
}
