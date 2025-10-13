package com.mvdown

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.mvdown.api.ApiClient
import com.mvdown.models.FormatRequest
import com.mvdown.ui.activities.ServerFilesActivity
import com.mvdown.ui.bottom_sheets.FormatBottomSheet
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etUrl: TextInputEditText
    private lateinit var btnFetchFormats: MaterialButton
    private lateinit var btnDownloads: MaterialButton
    private lateinit var btnServerFiles: MaterialButton
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupToolbar()
        setupListeners()
        handleSharedIntent()
        animateEntrance()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etUrl = findViewById(R.id.etUrl)
        btnFetchFormats = findViewById(R.id.btnFetchFormats)
        btnDownloads = findViewById(R.id.btnDownloads)
        btnServerFiles = findViewById(R.id.btnServerFiles)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    // TODO: Open settings
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupListeners() {
        btnFetchFormats.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty() && !isProcessing) {
                fetchFormats(url)
            } else if (url.isEmpty()) {
                etUrl.error = "Please enter a URL"
                etUrl.requestFocus()
            }
        }

        btnDownloads.setOnClickListener {
            if (!isProcessing) {
                val intent = Intent(this, DownloadsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        btnServerFiles.setOnClickListener {
            if (!isProcessing) {
                val intent = Intent(this, ServerFilesActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }
    
    private fun handleSharedIntent() {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedUrl ->
                        etUrl.setText(sharedUrl)
                        Toast.makeText(this, "URL received from share", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun fetchFormats(url: String) {
        setProcessingState(true)
        
        lifecycleScope.launch {
            try {
                val request = FormatRequest(url = url)
                val response = ApiClient.service.getFormats(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val formatResponse = response.body()!!
                    val formats = formatResponse.videoFormats
                    
                    setProcessingState(false)
                    
                    FormatBottomSheet.newInstance(formats, url)
                        .show(supportFragmentManager, "FormatBottomSheet")
                } else {
                    setProcessingState(false)
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(
                        this@MainActivity,
                        "Error: $errorMsg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                setProcessingState(false)
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadAudio(url : String){
        setProcessingState(true)
        val request = DownloadRequest(url)
        val response = ApiClient.service.downloadBestAudio(request)

        if (response.isSuccessful && response.body != null){
            
        }
    }

    private fun downloadVideo(){}
    private fun setProcessingState(processing: Boolean) {
        isProcessing = processing
        btnFetchFormats.isEnabled = !processing
        btnDownloads.isEnabled = !processing
        btnServerFiles.isEnabled = !processing
        
        if (processing) {
            btnFetchFormats.text = "Processing..."
            btnFetchFormats.icon = null
        } else {
            btnFetchFormats.text = "Fetch Formats"
            btnFetchFormats.setIconResource(R.drawable.ic_search)
        }
    }
    
    private fun animateEntrance() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        
        findViewById<View>(R.id.urlInputLayout).startAnimation(slideUp)
        btnFetchFormats.startAnimation(fadeIn)
        btnDownloads.startAnimation(fadeIn)
        btnServerFiles.startAnimation(fadeIn)
    }
}
