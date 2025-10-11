package com.mvdown

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mvdown.api.ApiClient
import com.mvdown.models.FormatRequest
import com.mvdown.ui.bottom_sheets.FormatBottomSheet
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var etUrl: EditText
    private lateinit var btnFetchFormats: Button
    private lateinit var btnDownloads: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        etUrl = findViewById(R.id.etUrl)
        btnFetchFormats = findViewById(R.id.btnFetchFormats)
        btnDownloads = findViewById(R.id.btnDownloads)

        // Set click listeners
        btnFetchFormats.setOnClickListener {
            val url = etUrl.text.toString()
            if (url.isNotEmpty()) {
                fetchFormats(url)
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnDownloads.setOnClickListener {
            // TODO: Implement downloads view
            Toast.makeText(this, "Opening downloads...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFormats(url: String) {
        lifecycleScope.launch {
            try {
                // Create FormatRequest object with the URL
                val request = FormatRequest(url = url)
                val response = ApiClient.service.getFormats(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val formats = response.body()!!
                    FormatBottomSheet.newInstance(formats)
                        .show(supportFragmentManager, "FormatBottomSheet")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(
                        this@MainActivity,
                        "Error: $errorMsg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
