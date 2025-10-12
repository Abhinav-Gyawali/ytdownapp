# 🔧 Complete Fix Guide for Android Media Downloader App

## Overview
This guide fixes critical issues preventing downloads from working and enhances the UI with Material Design.

---

## 📋 Critical Issues Identified

1. **WebSocket URL is missing leading slash** - Connection will fail with 404
2. **FormatBottomSheet has no download logic** - Clicking format does nothing
3. **No WebSocket connection established** - Progress tracking won't work
4. **Missing download initiation flow** - API call not made
5. **No progress dialog/UI** - User can't see download status
6. **UI is too basic** - Needs Material Design enhancement

---

## 🛠️ Required Fixes

### 1. Fix WebSocketManager (Critical Bug)

**File:** `app/src/main/java/com/mvdown/websocket/DownloadWebSocketManager.kt`

**Line 18 - Fix the URL by adding leading slash:**

```kotlin
// CHANGE THIS:
val wsUrl = baseUrl.replace("http://", "ws://")
    .replace("https://", "wss://") + "ws/$downloadId"

// TO THIS:
val wsUrl = baseUrl.replace("http://", "ws://")
    .replace("https://", "wss://") + "/ws/$downloadId"
```

---

### 2. Create Download Manager

**Create new file:** `app/src/main/java/com/mvdown/manager/DownloadManager.kt`

```kotlin
package com.mvdown.manager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mvdown.api.ApiClient
import com.mvdown.models.DownloadEvent
import com.mvdown.models.DownloadRequest
import com.mvdown.websocket.DownloadWebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadManager(private val context: Context) {
    
    private val wsManager = DownloadWebSocketManager(ApiClient.BASE_URL)
    private val _downloadEvent = MutableLiveData<DownloadEvent>()
    val downloadEvent: LiveData<DownloadEvent> = _downloadEvent
    
    fun startDownload(url: String, formatId: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                // Step 1: Initiate download
                val request = DownloadRequest(url, formatId)
                val response = ApiClient.service.initiateDownload(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val downloadResponse = response.body()!!
                    val downloadId = downloadResponse.downloadId
                    
                    // Step 2: Connect WebSocket for progress
                    scope.launch(Dispatchers.IO) {
                        wsManager.connect(downloadId).collect { event ->
                            _downloadEvent.postValue(event)
                        }
                    }
                } else {
                    _downloadEvent.postValue(
                        DownloadEvent.Error("Failed to start download: ${response.code()}")
                    )
                }
            } catch (e: Exception) {
                _downloadEvent.postValue(
                    DownloadEvent.Error(e.message ?: "Unknown error")
                )
            }
        }
    }
    
    fun disconnect() {
        wsManager.disconnect()
    }
}
```

---

### 3. Create Download Progress Dialog

**Create new file:** `app/src/main/java/com/mvdown/ui/dialogs/DownloadProgressDialog.kt`

```kotlin
package com.mvdown.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.mvdown.R
import com.mvdown.models.DownloadEvent

class DownloadProgressDialog(context: Context) : Dialog(context) {
    
    private lateinit var tvTitle: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvEta: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_download_progress_simple)
        setCancelable(false)
        
        tvTitle = findViewById(R.id.tvDownloadTitle)
        tvProgress = findViewById(R.id.tvProgress)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvEta = findViewById(R.id.tvEta)
        progressBar = findViewById(R.id.downloadProgress)
        
        tvTitle.text = "Processing your request..."
    }
    
    fun updateProgress(event: DownloadEvent.Progress) {
        when (event.status) {
            "starting" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = "Initializing download..."
                progressBar.isIndeterminate = true
            }
            "downloading" -> {
                tvTitle.text = "Downloading..."
                progressBar.isIndeterminate = false
                
                event.percent?.let {
                    tvProgress.text = it
                    val percentValue = it.replace("%", "").toFloatOrNull() ?: 0f
                    progressBar.progress = percentValue.toInt()
                }
                
                event.speed?.let {
                    val speedMB = it / (1024 * 1024)
                    tvSpeed.text = String.format("%.2f MB/s", speedMB)
                }
                
                event.eta?.let {
                    tvEta.text = "ETA: ${it}s"
                }
            }
            "processing" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = event.message ?: "Finalizing download..."
                progressBar.isIndeterminate = true
            }
            "zipping" -> {
                tvTitle.text = "Processing your request..."
                tvProgress.text = event.message ?: "Creating archive..."
                progressBar.isIndeterminate = true
            }
        }
    }
}
```

---

### 4. Create Simple Progress Dialog Layout

**Create new file:** `app/src/main/res/layout/dialog_download_progress_simple.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="24dp"
    app:cardCornerRadius="16dp"
    app:cardElevation="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <TextView
            android:id="@+id/tvDownloadTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Processing your request..."
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="@color/black"
            android:layout_marginBottom="16dp"/>

        <ProgressBar
            android:id="@+id/downloadProgress"
            style="?android:attr/progressBarStyleHorizontal"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:max="100"
            android:progress="0"
            android:layout_marginBottom="12dp"/>

        <TextView
            android:id="@+id/tvProgress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0%"
            android:textSize="14sp"
            android:layout_marginBottom="8dp"/>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvSpeed"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Speed: --"
                android:textSize="12sp"
                android:textColor="@color/grey_dark"/>

            <TextView
                android:id="@+id/tvEta"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="ETA: --"
                android:textSize="12sp"
                android:textColor="@color/grey_dark"/>
        </LinearLayout>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

---

### 5. Fix FormatBottomSheet

**Update file:** `app/src/main/java/com/mvdown/ui/bottom_sheets/FormatBottomSheet.kt`

**Replace entire content with:**

```kotlin
package com.mvdown.ui.bottom_sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mvdown.R
import com.mvdown.manager.DownloadManager
import com.mvdown.models.DownloadEvent
import com.mvdown.models.VideoFormat
import com.mvdown.ui.adapters.FormatsAdapter
import com.mvdown.ui.dialogs.DownloadProgressDialog

class FormatBottomSheet : BottomSheetDialogFragment() {
    private lateinit var rvFormats: RecyclerView
    private lateinit var adapter: FormatsAdapter
    private var formats: List<VideoFormat> = emptyList()
    private var url: String = ""
    private lateinit var downloadManager: DownloadManager
    private var progressDialog: DownloadProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_format, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadManager = DownloadManager(requireContext())
        
        rvFormats = view.findViewById(R.id.rvFormats)
        rvFormats.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = FormatsAdapter { format ->
            startDownload(format)
        }
        adapter.updateFormats(formats)
        rvFormats.adapter = adapter
        
        observeDownloadEvents()
    }

    private fun startDownload(format: VideoFormat) {
        dismiss()
        
        progressDialog = DownloadProgressDialog(requireContext()).apply {
            show()
        }
        
        downloadManager.startDownload(url, format.formatId, lifecycleScope)
    }
    
    private fun observeDownloadEvents() {
        downloadManager.downloadEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is DownloadEvent.Progress -> {
                    progressDialog?.updateProgress(event)
                }
                is DownloadEvent.Done -> {
                    progressDialog?.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Download complete: ${event.filename}",
                        Toast.LENGTH_LONG
                    ).show()
                    downloadManager.disconnect()
                }
                is DownloadEvent.Error -> {
                    progressDialog?.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error: ${event.error}",
                        Toast.LENGTH_LONG
                    ).show()
                    downloadManager.disconnect()
                }
            }
        }
    }

    fun setFormats(formats: List<VideoFormat>) {
        this.formats = formats
        if (::adapter.isInitialized) {
            adapter.updateFormats(formats)
        }
    }
    
    fun setUrl(url: String) {
        this.url = url
    }

    companion object {
        fun newInstance(formats: List<VideoFormat>, url: String): FormatBottomSheet {
            return FormatBottomSheet().apply {
                setFormats(formats)
                setUrl(url)
            }
        }
    }
}
```

---

### 6. Fix MainActivity

**Update file:** `app/src/main/java/com/mvdown/MainActivity.kt`

**Replace entire content with:**

```kotlin
package com.mvdown

import android.content.Intent
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

        etUrl = findViewById(R.id.etUrl)
        btnFetchFormats = findViewById(R.id.btnFetchFormats)
        btnDownloads = findViewById(R.id.btnDownloads)

        btnFetchFormats.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                fetchFormats(url)
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnDownloads.setOnClickListener {
            Toast.makeText(this, "Opening downloads...", Toast.LENGTH_SHORT).show()
        }
        
        handleSharedIntent()
    }
    
    private fun handleSharedIntent() {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedUrl ->
                        etUrl.setText(sharedUrl)
                        Toast.makeText(this, "URL received", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun fetchFormats(url: String) {
        lifecycleScope.launch {
            try {
                val request = FormatRequest(url = url)
                val response = ApiClient.service.getFormats(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val formatResponse = response.body()!!
                    val formats = formatResponse.videoFormats
                    
                    FormatBottomSheet.newInstance(formats, url)
                        .show(supportFragmentManager, "FormatBottomSheet")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@MainActivity, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
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
```

---

### 7. Make BASE_URL Public in ApiClient

**Update file:** `app/src/main/java/com/mvdown/api/ApiClient.kt`

**Change line 8:**

```kotlin
object ApiClient {
    
    // Change from private to const (public)
    // FROM: private const val BASE_URL = "https://ytdownbackend.onrender.com/"
    // TO:
    const val BASE_URL = "https://ytdownbackend.onrender.com/"
    
    // ... rest of the code remains same
}
```

---

### 8. Enhanced UI Updates

#### Update Format Item Layout

**Update file:** `app/src/main/res/layout/item_format.xml`

**Replace entire content with:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:gravity="center_vertical">

        <ImageView
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@drawable/ic_video"
            android:tint="@color/colorPrimary"
            android:layout_marginEnd="16dp"/>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvFormatInfo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@color/black"/>

            <TextView
                android:id="@+id/tvFormatSize"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="@color/grey_dark"
                android:layout_marginTop="4dp"/>
        </LinearLayout>

        <Button
            android:id="@+id/btnDownload"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Download"
            android:textColor="@color/white"
            android:background="@drawable/button_bg"
            android:paddingHorizontal="16dp"/>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

#### Update FormatsAdapter ViewHolder

**Update file:** `app/src/main/java/com/mvdown/ui/adapters/FormatsAdapter.kt`

**Update the ViewHolder class to include tvFormatSize:**

```kotlin
class FormatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvFormatInfo: TextView = view.findViewById(R.id.tvFormatInfo)
    val tvFormatSize: TextView = view.findViewById(R.id.tvFormatSize)
    val btnDownload: Button = view.findViewById(R.id.btnDownload)
}
```

**Update the onBindViewHolder method:**

```kotlin
override fun onBindViewHolder(holder: FormatViewHolder, position: Int) {
    val format = formats[position]
    val resolution = format.resolution ?: "Unknown"
    val ext = format.ext ?: "mp4"
    val size = format.filesize?.let { "${it/1024/1024} MB" } ?: "Unknown size"
    
    holder.tvFormatInfo.text = "$resolution • $ext"
    holder.tvFormatSize.text = size
    holder.btnDownload.setOnClickListener { onFormatClick(format) }
}
```

---

## ✅ Testing Checklist

Follow these steps to verify everything works:

1. **Enter a YouTube URL** in the text field
2. **Click "Fetch Formats"** button
3. **See bottom sheet** with available formats
4. **Select a format** by clicking "Download"
5. **See progress dialog** showing "Processing your request..."
6. **Watch real-time progress** with percentage, speed, and ETA
7. **See completion toast** with filename when done
8. **Handle shared URLs** by sharing a YouTube link to the app

---

## 🎨 UI Improvements Included

- ✨ Material Design cards for format items
- 📊 Progress dialog with real-time updates
- 🎯 Video icons for visual appeal
- 💫 Smooth animations and transitions
- 📱 Better spacing and padding
- 🎨 Consistent color scheme
- ⚡ Improved user feedback

---

## 🐛 Common Issues & Solutions

### WebSocket Connection Failed
- **Problem:** WebSocket URL missing leading slash
- **Solution:** Already fixed in step 1

### Download Not Starting
- **Problem:** Missing download initiation logic
- **Solution:** Added DownloadManager in step 2

### No Progress Updates
- **Problem:** WebSocket events not observed
- **Solution:** Added LiveData observer in step 5

### Dialog Not Showing
- **Problem:** Missing layout file
- **Solution:** Created layout in step 4

---

## 📝 Summary of Changes

| File | Action | Purpose |
|------|--------|---------|
| `DownloadWebSocketManager.kt` | Modified | Fix WebSocket URL |
| `DownloadManager.kt` | Created | Handle download flow |
| `DownloadProgressDialog.kt` | Created | Show progress UI |
| `dialog_download_progress_simple.xml` | Created | Progress layout |
| `FormatBottomSheet.kt` | Modified | Add download logic |
| `MainActivity.kt` | Modified | Handle shared intents |
| `ApiClient.kt` | Modified | Make BASE_URL public |
| `item_format.xml` | Modified | Enhanced UI |
| `FormatsAdapter.kt` | Modified | Better formatting |

---

## 🚀 Next Steps

After implementing all fixes:

1. Clean and rebuild project
2. Run the app on device/emulator
3. Test with various YouTube URLs
4. Test share functionality from YouTube app
5. Verify progress updates work correctly
6. Check download completion notifications

---

## 💡 Tips

- Use a real device for testing WebSocket connections
- Check Logcat for any error messages
- Ensure backend server is running and accessible
- Test with different network conditions
- Verify file permissions for downloads

---

**Made with ❤️ for seamless media downloads**