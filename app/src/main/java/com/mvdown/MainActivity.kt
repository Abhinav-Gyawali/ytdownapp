package com.mvdown

import android.content.Intent
import android.os.Bundle
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
    private var isShowingDownloads = true

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

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "M/V Down"
    }

    private fun setupRecyclerViews() {
        // File adapter
        fileAdapter = FileAdapter(
            onDownload = { file -> downloadFile(file) },
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
            if (isShowingDownloads) {
                loadFiles()
            } else {
                loadFiles()
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
                
                showSnackbar("Download started: ${response.downloadId}")
                // Navigate to downloads tab
                showDownloads()
                
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
                showSnackbar("Cannot connect to API")
            }
        }
    }

    private fun loadFiles() {
        lifecycleScope.launch {
            try {
                val files = ApiClient.apiService.getFiles()
                fileAdapter.submitList(files)
                binding.swipeRefresh.isRefreshing = false
            } catch (e: Exception) {
                showSnackbar("Error loading files: ${e.message}")
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun downloadFile(fileName: String) {
        val url = "${ApiClient.BASE_URL}/downloads/$fileName"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(android.net.Uri.parse(url), "*/*")
        startActivity(intent)
    }

    private fun deleteServerFile(fileName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete $fileName?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ApiClient.apiService.deleteServerFile(fileName)
                        showSnackbar("File deleted")
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
        isShowingDownloads = false
        binding.recyclerView.adapter = fileAdapter
        binding.chipFiles.isChecked = true
        binding.chipDownloads.isChecked = false
        loadFiles()
    }

    private fun showDownloads() {
        isShowingDownloads = true
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
            .setMessage("This will delete all downloaded files. Continue?")
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