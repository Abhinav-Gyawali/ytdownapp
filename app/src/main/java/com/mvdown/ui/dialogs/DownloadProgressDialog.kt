package com.mvdown.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mvdown.R
import com.mvdown.models.DownloadEvent

class DownloadProgressDialog(context: Context) : Dialog(context) {
    
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvEta: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fileSizeCard: MaterialCardView
    private lateinit var btnCancel: MaterialButton
    
    private var onCancelListener: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_download_progress_simple)
        setCancelable(true)
        
        initViews()
        setupListeners()
        setInitialState()
    }
    
    private fun initViews() {
        tvTitle = findViewById(R.id.tvDownloadTitle)
        tvSubtitle = findViewById(R.id.tvDownloadSubtitle)
        tvProgress = findViewById(R.id.tvProgress)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvEta = findViewById(R.id.tvEta)
        tvFileSize = findViewById(R.id.tvFileSize)
        progressBar = findViewById(R.id.downloadProgress)
        fileSizeCard = findViewById(R.id.fileSizeCard)
        btnCancel = findViewById(R.id.btnCancel)
    }
    
    private fun setupListeners() {
        btnCancel.setOnClickListener {
            onCancelListener?.invoke()
            dismiss()
        }
        
        setOnCancelListener {
            onCancelListener?.invoke()
        }
    }
    
    private fun setInitialState() {
        tvTitle.text = "Processing your request..."
        tvSubtitle.text = "Preparing download..."
        tvProgress.text = "0%"
        tvSpeed.text = "--"
        tvEta.text = "--"
        progressBar.isIndeterminate = true
        progressBar.progress = 0
        fileSizeCard.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
    }
    
    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }
    
    fun updateProgress(event: DownloadEvent.Progress) {
        when (event.status) {
            "starting", "connected" -> {
                tvTitle.text = "Initializing Download"
                tvSubtitle.text = event.message ?: "Connecting to server..."
                tvProgress.text = "0%"
                tvSpeed.text = "--"
                tvEta.text = "--"
                progressBar.isIndeterminate = true
                progressBar.progress = 0
                fileSizeCard.visibility = View.GONE
            }
            
            "downloading" -> {
                tvTitle.text = "Downloading"
                tvSubtitle.text = "Please wait while we download your file"
                progressBar.isIndeterminate = false
                fileSizeCard.visibility = View.VISIBLE
                
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
                        String.format("%.2f MB/s", speedMB)
                    } else {
                        "--"
                    }
                } ?: run {
                    tvSpeed.text = "--"
                }
                
                // Update ETA
                event.eta?.let { etaSeconds ->
                    tvEta.text = if (etaSeconds > 0) {
                        formatETA(etaSeconds)
                    } else {
                        "--"
                    }
                } ?: run {
                    tvEta.text = "--"
                }
                
                // Show downloaded/total if available
                if (event.downloadedBytes != null && event.totalBytes != null && event.totalBytes > 0) {
                    val downloadedMB = event.downloadedBytes / (1024.0 * 1024.0)
                    val totalMB = event.totalBytes / (1024.0 * 1024.0)
                    tvFileSize.text = String.format("%.1f MB / %.1f MB", downloadedMB, totalMB)
                } else {
                    tvFileSize.text = "Calculating..."
                }
            }
            
            "processing" -> {
                tvTitle.text = "Processing"
                tvSubtitle.text = event.message ?: "Finalizing your download..."
                tvProgress.text = "100%"
                tvSpeed.text = "--"
                tvEta.text = "--"
                progressBar.isIndeterminate = true
                progressBar.progress = 100
                fileSizeCard.visibility = View.GONE
            }
            
            "zipping" -> {
                tvTitle.text = "Creating Archive"
                tvSubtitle.text = event.message ?: "Compressing files into archive..."
                tvProgress.text = "100%"
                tvSpeed.text = "--"
                tvEta.text = "--"
                progressBar.isIndeterminate = true
                progressBar.progress = 100
                fileSizeCard.visibility = View.GONE
            }
            
            "closed" -> {
                tvTitle.text = "Connection Closed"
                tvSubtitle.text = event.message ?: "The connection was closed"
                tvProgress.text = "0%"
                tvSpeed.text = "--"
                tvEta.text = "--"
                progressBar.isIndeterminate = false
                progressBar.progress = 0
                fileSizeCard.visibility = View.GONE
            }
            
            else -> {
                tvTitle.text = "Processing"
                tvSubtitle.text = event.message ?: event.status
                tvSpeed.text = "--"
                tvEta.text = "--"
                progressBar.isIndeterminate = true
                fileSizeCard.visibility = View.GONE
            }
        }
    }
    
    /**
     * Format ETA in a human-readable format
     * @param seconds ETA in seconds
     * @return Formatted string like "1m 30s" or "45s"
     */
    private fun formatETA(seconds: Int): String {
        return when {
            seconds < 0 -> "--"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                if (secs > 0) "${minutes}m ${secs}s" else "${minutes}m"
            }
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
        }
    }
    
    /**
     * Show completion state
     */
    fun showComplete(filename: String) {
        tvTitle.text = "Download Complete! ✓"
        tvSubtitle.text = filename
        tvProgress.text = "100%"
        tvSpeed.text = "Complete"
        tvEta.text = "Done"
        progressBar.isIndeterminate = false
        progressBar.progress = 100
        fileSizeCard.visibility = View.GONE
        btnCancel.visibility = View.GONE
    }
    
    /**
     * Show error state
     */
    fun showError(error: String) {
        tvTitle.text = "Download Failed ✗"
        tvSubtitle.text = error
        tvProgress.text = "0%"
        tvSpeed.text = "--"
        tvEta.text = "--"
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        fileSizeCard.visibility = View.GONE
        btnCancel.text = "Close"
    }
}
