package com.mvdown

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mvdown.adapter.FormatAdapter
import com.mvdown.databinding.ActivityDownloadBinding
import com.mvdown.model.DownloadRequest
import com.mvdown.model.FormatRequest
import com.mvdown.network.ApiClient
import kotlinx.coroutines.launch

class DownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadBinding
    private lateinit var formatAdapter: FormatAdapter
    private var selectedFormatId: String? = null
    private var currentUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Download Media"
    }

    private fun setupRecyclerView() {
        formatAdapter = FormatAdapter { format ->
            selectedFormatId = format.formatId
            binding.btnDownload.isEnabled = true
        }

        binding.recyclerFormats.apply {
            layoutManager = GridLayoutManager(this@DownloadActivity, 2)
            adapter = formatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnGetFormats.setOnClickListener {
            fetchFormats()
        }

        binding.btnDownload.setOnClickListener {
            startDownload()
        }
    }

    private fun fetchFormats() {
        currentUrl = binding.etUrl.text.toString().trim()
        
        if (currentUrl.isEmpty()) {
            showSnackbar("Please enter a URL")
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.show()
                binding.layoutFormats.visibility = android.view.View.GONE

                val response = ApiClient.apiService.getFormats(FormatRequest(currentUrl))
                
                binding.tvTitle.text = response.title
                formatAdapter.submitFormats(response)
                binding.layoutFormats.visibility = android.view.View.VISIBLE
                
            } catch (e: Exception) {
                showSnackbar("Error: ${e.message}")
            } finally {
                binding.progressBar.hide()
            }
        }
    }

    private fun startDownload() {
        if (selectedFormatId == null) {
            showSnackbar("Please select a format")
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.show()
                
                val response = ApiClient.apiService.initiateDownload(
                    DownloadRequest(currentUrl, selectedFormatId!!)
                )
                
                showSnackbar("Download started!")
                
                // Navigate to progress screen
                val intent = android.content.Intent(
                    this@DownloadActivity,
                    ProgressActivity::class.java
                )
                intent.putExtra("download_id", response.downloadId)
                startActivity(intent)
                finish()
                
            } catch (e: Exception) {
                showSnackbar("Error: ${e.message}")
            } finally {
                binding.progressBar.hide()
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
