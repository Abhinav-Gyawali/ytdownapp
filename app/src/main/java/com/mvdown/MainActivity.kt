package com.mvdown



import android.os.Bundle

import android.widget.Button

import android.widget.EditTextimport android.os.Bundleimport android.os.Bundle

import android.widget.Toast

import androidx.appcompat.app.AppCompatActivityimport android.widget.Buttonimport android.widget.Button

import androidx.lifecycle.lifecycleScope

import com.mvdown.api.ApiClientimport android.widget.EditTextimport android.widget.EditText

import com.mvdown.ui.bottom_sheets.FormatBottomSheet

import kotlinx.coroutines.launchimport android.widget.Toastimport android.widget.Toast



class MainActivity : AppCompatActivity() {import androidx.appcompat.app.AppCompatActivityimport androidx.appcompat.app.AppCompatActivity

    private lateinit var etUrl: EditText

    private lateinit var btnFetchFormats: Buttonimport androidx.lifecycle.lifecycleScopeimport androidx.lifecycle.lifecycleScope

    private lateinit var btnDownloads: Button

import com.mvdown.api.ApiClientimport com.mvdown.api.ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)import com.mvdown.ui.bottom_sheets.FormatBottomSheetimport com.mvdown.ui.bottom_sheets.FormatBottomSheet

        setContentView(R.layout.activity_main)

import kotlinx.coroutines.launchimport kotlinx.coroutines.launch

        // Initialize views

        etUrl = findViewById(R.id.etUrl)

        btnFetchFormats = findViewById(R.id.btnFetchFormats)

        btnDownloads = findViewById(R.id.btnDownloads)class MainActivity : AppCompatActivity() {class MainActivity : AppCompatActivity() {



        // Set click listeners    private lateinit var etUrl: EditText    private lateinit var etUrl: EditText

        btnFetchFormats.setOnClickListener {

            val url = etUrl.text.toString()    private lateinit var btnFetchFormats: Button    private lateinit var btnFetchFormats: Button

            if (url.isNotEmpty()) {

                fetchFormats(url)    private lateinit var btnDownloads: Button    private lateinit var btnDownloads: Button

            } else {

                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()

            }

        }    override fun onCreate(savedInstanceState: Bundle?) {    override fun onCreate(savedInstanceState: Bundle?) {



        btnDownloads.setOnClickListener {        super.onCreate(savedInstanceState)        super.onCreate(savedInstanceState)

            // TODO: Implement downloads view

            Toast.makeText(this, "Opening downloads...", Toast.LENGTH_SHORT).show()        setContentView(R.layout.activity_main)        setContentView(R.layout.activity_main)

        }

    }



    private fun fetchFormats(url: String) {        // Initialize views        // Initialize views

        lifecycleScope.launch {

            try {        etUrl = findViewById(R.id.etUrl)        etUrl = findViewById(R.id.etUrl)

                val response = ApiClient.service.getFormats(url)

                if (response.isSuccessful && response.body() != null) {        btnFetchFormats = findViewById(R.id.btnFetchFormats)        btnFetchFormats = findViewById(R.id.btnFetchFormats)

                    val formats = response.body()!!

                    FormatBottomSheet.newInstance(formats)        btnDownloads = findViewById(R.id.btnDownloads)        btnDownloads = findViewById(R.id.btnDownloads)

                        .show(supportFragmentManager, "FormatBottomSheet")

                } else {

                    Toast.makeText(this@MainActivity, 

                        "Error: ${response.errorBody()?.string() ?: "Unknown error"}",         // Set click listeners        // Set click listeners

                        Toast.LENGTH_SHORT).show()

                }        btnFetchFormats.setOnClickListener {        btnFetchFormats.setOnClickListener {

            } catch (e: Exception) {

                Toast.makeText(this@MainActivity,             val url = etUrl.text.toString()            val url = etUrl.text.toString()

                    "Error: ${e.message ?: "Unknown error"}", 

                    Toast.LENGTH_SHORT).show()            if (url.isNotEmpty()) {            if (url.isNotEmpty()) {

            }

        }                fetchFormats(url)                fetchFormats(url)

    }

}            } else {            } else {

                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()

            }            }

        }        }



        btnDownloads.setOnClickListener {        btnDownloads.setOnClickListener {

            // TODO: Implement downloads view            // TODO: Implement downloads view

            Toast.makeText(this, "Opening downloads...", Toast.LENGTH_SHORT).show()            Toast.makeText(this, "Opening downloads...", Toast.LENGTH_SHORT).show()

        }        }

    }    }



    private fun fetchFormats(url: String) {    private fun fetchFormats(url: String) {

        lifecycleScope.launch {        lifecycleScope.launch {

            try {            try {

                val response = ApiClient.service.getFormats(url)                val response = ApiClient.service.getFormats(url)

                if (response.isSuccessful && response.body() != null) {                if (response.isSuccessful && response.body() != null) {

                    val formats = response.body()!!                    val formats = response.body()!!

                    FormatBottomSheet.newInstance(formats)                    FormatBottomSheet.newInstance(formats)

                        .show(supportFragmentManager, "FormatBottomSheet")                        .show(supportFragmentManager, "FormatBottomSheet")

                } else {                } else {

                    Toast.makeText(this@MainActivity,                     Toast.makeText(this@MainActivity, 

                        "Error: ${response.errorBody()?.string() ?: "Unknown error"}",                         "Error: ${response.errorBody()?.string() ?: "Unknown error"}", 

                        Toast.LENGTH_SHORT).show()                        Toast.LENGTH_SHORT).show()

                }                }

            } catch (e: Exception) {            } catch (e: Exception) {

                Toast.makeText(this@MainActivity,                 Toast.makeText(this@MainActivity, 

                    "Error: ${e.message ?: "Unknown error"}",                     "Error: ${e.message ?: "Unknown error"}", 

                    Toast.LENGTH_SHORT).show()                    Toast.LENGTH_SHORT).show()

            }            }

        }        }

    }    }

}}