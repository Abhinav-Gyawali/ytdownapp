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
    val downloads = mutableListOf<DownloadedFile>()

    val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.SIZE,
        MediaStore.Downloads.DATE_ADDED
    )

    val query = contentResolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        MediaStore.Downloads.DATE_ADDED + " DESC"
    )

    query?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
            downloads.add(
                DownloadedFile(
                    title = cursor.getString(nameColumn),
                    filePath = uri.toString(),
                    downloadDate = cursor.getLong(dateColumn) * 1000, // Convert seconds to ms
                    size = cursor.getLong(sizeColumn)
                )
            )
        }
    }

    adapter.updateDownloads(downloads)
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
