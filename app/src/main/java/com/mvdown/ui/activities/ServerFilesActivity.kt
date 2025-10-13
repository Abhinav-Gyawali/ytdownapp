package com.mvdown.ui.activities

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mvdown.R
import com.mvdown.adapters.ServerFileAdapter
import com.mvdown.api.ApiClient
import com.mvdown.models.FileInfo
import kotlinx.coroutines.launch

class ServerFilesActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvServerFiles: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: ServerFileAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_files)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        loadServerFiles()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvServerFiles = findViewById(R.id.rvServerFiles)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Server Files"
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ServerFileAdapter { fileInfo ->
            downloadFileFromServer(fileInfo)
        }
        rvServerFiles.layoutManager = LinearLayoutManager(this)
        rvServerFiles.adapter = adapter
    }
    
    private fun loadServerFiles() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.service.getFiles()
                
                if (response.isSuccessful && response.body() != null) {
                    val files = response.body()!!
                    
                    if (files.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = "No files available on server"
                    } else {
                        adapter.updateFiles(files)
                    }
                } else {
                    Toast.makeText(
                        this@ServerFilesActivity,
                        "Failed to load files: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Failed to load files"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ServerFilesActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Error loading files"
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun downloadFileFromServer(fileInfo: FileInfo) {
        try {
            val downloadUrl = "${ApiClient.BASE_URL}downloads/${fileInfo.name}"
            
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle(fileInfo.name)
                .setDescription("Downloading from server")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileInfo.name)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            
            Toast.makeText(
                this,
                "Downloading ${fileInfo.name}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to start download: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
