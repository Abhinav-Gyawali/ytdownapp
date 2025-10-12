package com.mvdown

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mvdown.adapters.DownloadedFileAdapter
import com.mvdown.models.DownloadedFile
import java.io.File

class DownloadsActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvDownloads: RecyclerView
    private lateinit var adapter: DownloadedFileAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        loadDownloads()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvDownloads = findViewById(R.id.rvDownloads)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "My Downloads"
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DownloadedFileAdapter()
        rvDownloads.layoutManager = LinearLayoutManager(this)
        rvDownloads.adapter = adapter
    }
    
    private fun loadDownloads() {
        // TODO: Implement actual downloads loading
        val sampleDownloads = listOf(
            DownloadedFile(
                title = "Sample Video 1",
                filePath = "/storage/emulated/0/Download/sample1.mp4",
                downloadDate = System.currentTimeMillis(),
                size = 1024 * 1024 * 50 // 50MB
            ),
            DownloadedFile(
                title = "Sample Video 2",
                filePath = "/storage/emulated/0/Download/sample2.mp4",
                downloadDate = System.currentTimeMillis() - 86400000, // Yesterday
                size = 1024 * 1024 * 100 // 100MB
            )
        )
        
        adapter.updateDownloads(sampleDownloads)
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