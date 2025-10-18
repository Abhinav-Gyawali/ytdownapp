package com.mvdown

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mvdown.adapter.DownloadAdapter
import com.mvdown.adapter.FileAdapter
import com.mvdown.databinding.ActivityMainBinding
import com.mvdown.model.DownloadRequest
import com.mvdown.network.ApiClient
import com.mvdown.util.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var fileAdapter: FileAdapter
    private lateinit var downloadAdapter: DownloadAdapter
    private var isShowingFiles = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize preferences
        preferenceManager = PreferenceManager(this)
        
        // Apply theme
        applyTheme()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerViews()
        setupListeners()
        loadApiHealth()
        loadFiles()
    }

    override fun onResume() {
        super.onResume()
        // Refresh files when returning to activity
        if (isShowingFiles) {
            loadFiles()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "M/V Down"
    }

    private fun setupRecyclerViews() {
        // File adapter
        fileAdapter = FileAdapter(
            onDownload = { file -> downloadFileToDevice(file) },
            onDelete = { file -> deleteServerFile(file) }
        )
        
        // Download adapter
        downloadAdapter = DownloadAdapter()
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = fileAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            showDownloadDialog()
        }

        binding.btnQuickAudio.setOnClickListener {
            quickDownload("best-audio")
        }

        binding.btnQuickVideo.setOnClickListener {
            quickDownload("best-video")
        }

        binding.chipFiles.setOnClickListener {
            showFiles()
        }

        binding.chipDownloads.setOnClickListener {
            showDownloads()
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (isShowingFiles) {
                loadFiles()
            } else {
                // Load active downloads
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showDownloadDialog() {
        val intent = Intent(this, DownloadActivity::class.java)
        startActivity(intent)
    }

    private fun quickDownload(formatId: String) {
        val url = binding.urlInput.text.toString().trim()
        
        if (url.isEmpty()) {
            showSnackbar("Please enter a URL")
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressIndicator.show()
                val response = ApiClient.apiService.initiateDownload(
                    DownloadRequest(url, formatId)
                )
                
                showSnackbar("Download started!")
                
                // Navigate to progress activity
                val intent = Intent(this@MainActivity, ProgressActivity::class.java)
                intent.putExtra("download_id", response.downloadId)
                startActivity(intent)
                
            } catch (e: Exception) {
                showSnackbar("Error: ${e.message}")
            } finally {
                binding.progressIndicator.hide()
            }
        }
    }

    private fun loadApiHealth() {
        lifecycleScope.launch {
            try {
                val health = ApiClient.apiService.getHealth()
                binding.tvApiStatus.text = "API: ${health.status}"
                binding.tvCookiesStatus.text = "Cookies: ${health.cookies}"
            } catch (e: Exception) {
                binding.tvApiStatus.text = "API: Offline"
                binding.tvCookiesStatus.text = "Cookies: Unknown"
                showSnackbar("Cannot connect to API: ${e.message}")
            }
        }
    }

    private fun loadFiles() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val files = ApiClient.apiService.getFiles()
                fileAdapter.submitList(files)
                
                if (files.isEmpty()) {
                    showSnackbar("No files available on server")
                }
            } catch (e: Exception) {
                showSnackbar("Error loading files: ${e.message}")
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
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
            val downloadId = downloadManager.enqueue(request)

            showSnackbar("ðŸ“¥ Downloading: $fileName")
            
        } catch (e: Exception) {
            showSnackbar("Download error: ${e.message}")
        }
    }

    private fun deleteServerFile(fileName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete File")
            .setMessage("Delete $fileName from server?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ApiClient.apiService.deleteServerFile(fileName)
                        showSnackbar("File deleted from server")
                        loadFiles()
                    } catch (e: Exception) {
                        showSnackbar("Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFiles() {
        isShowingFiles = true
        binding.recyclerView.adapter = fileAdapter
        binding.chipFiles.isChecked = true
        binding.chipDownloads.isChecked = false
        loadFiles()
    }

    private fun showDownloads() {
        isShowingFiles = false
        binding.recyclerView.adapter = downloadAdapter
        binding.chipFiles.isChecked = false
        binding.chipDownloads.isChecked = true
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun applyTheme() {
        val theme = preferenceManager.getTheme()
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                loadFiles()
                loadApiHealth()
                true
            }
            R.id.action_delete_all -> {
                deleteAllFiles()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteAllFiles() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete All Files")
            .setMessage("Delete all files from server?")
            .setPositiveButton("Delete All") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val result = ApiClient.apiService.deleteAllFiles()
                        showSnackbar("Deleted ${result.deletedCount} files")
                        loadFiles()
                    } catch (e: Exception) {
                        showSnackbar("Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}