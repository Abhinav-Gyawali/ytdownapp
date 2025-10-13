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
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val files = downloadsDir?.listFiles()

    if (files.isNullOrEmpty()) {
        Toast.makeText(this, "No downloads found", Toast.LENGTH_SHORT).show()
        adapter.updateDownloads(emptyList())
        return
    }

    val downloadedFiles = files.filter { it.isFile }.map { file ->
        DownloadedFile(
            title = file.name,
            filePath = file.absolutePath,
            downloadDate = file.lastModified(),
            size = file.length()
        )
    }

    adapter.updateDownloads(downloadedFiles)
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
