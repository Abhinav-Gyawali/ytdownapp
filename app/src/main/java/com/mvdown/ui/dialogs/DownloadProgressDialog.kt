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
        
        tvTitle = findViewById(R.id.tvDownloadTitle)
        tvProgress = findViewById(R.id.tvProgress)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvEta = findViewById(R.id.tvEta)
        progressBar = findViewById(R.id.downloadProgress)
        
        tvTitle.text = "Processing your request..."
    }
    
    fun updateProgress(event: DownloadEvent.Progress) {
        when (event.status) {
            "starting" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = "Initializing download..."
                progressBar.isIndeterminate = true
            }
            "downloading" -> {
                tvTitle.text = "Downloading..."
                progressBar.isIndeterminate = false
                
                event.percent?.let {
                    tvProgress.text = it
                    val percentValue = it.replace("%", "").toDoubleOrNull() ?: 0.0
                    progressBar.progress = percentValue.toInt()
                }
                
                event.speed?.let {
                    val speedMB = it / (1024 * 1024)
                    tvSpeed.text = String.format("%.2f MB/s", speedMB)
                }
                
                event.eta?.let {
                    tvEta.text = "ETA: ${it}s"
                }
            }
            "processing" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = event.message ?: "Finalizing download..."
                progressBar.isIndeterminate = true
            }
            "zipping" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = event.message ?: "Creating archive..."
                progressBar.isIndeterminate = true
            }
        }
    }
}