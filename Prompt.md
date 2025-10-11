# YouTube Downloader Android App - API Integration Corrections

## Backend API Endpoints Overview

Your FastAPI backend (`ytdownbackend`) exposes the following endpoints:

### 1. **POST /api/formats**
- **Purpose**: Fetch available video/audio formats for a given URL
- **Request Body**: `{ "url": "string" }`
- **Response**:
```json
{
  "title": "Video Title",
  "url": "original_url",
  "video_formats": [
    {
      "format_id": "string",
      "ext": "string",
      "resolution": "string",
      "filesize": number
    }
  ],
  "audio_formats": [
    {
      "format_id": "string",
      "ext": "string",
      "abr": number,
      "filesize": number
    }
  ]
}
```

### 2. **POST /api/download**
- **Purpose**: Initiate a download and get a WebSocket connection ID
- **Request Body**: `{ "url": "string", "format_id": "string" }`
- **Response**:
```json
{
  "download_id": "uuid",
  "websocket_url": "/ws/{download_id}",
  "message": "Download initiated..."
}
```

### 3. **WebSocket /ws/{download_id}**
- **Purpose**: Real-time download progress updates
- **Events Received**:
  - `{ "event": "progress", "status": "downloading", "percent": "50%", ... }`
  - `{ "event": "progress", "status": "processing", "message": "..." }`
  - `{ "event": "done", "title": "...", "filename": "...", "download_url": "/downloads/..." }`
  - `{ "event": "error", "error": "error message" }`

### 4. **GET /downloads/{filename}**
- **Purpose**: Download the actual file
- **Supports**: Range requests for resumable downloads
- **Response**: File stream

### 5. **GET /files**
- **Purpose**: Get list of all downloaded files
- **Response**:
```json
[
  {
    "name": "filename.mp4",
    "size": 12345678,
    "type": "video|audio|others"
  }
]
```

### 6. **DELETE /api/files/{filename}**
- **Purpose**: Delete a specific file
- **Response**: `{ "message": "File deleted successfully", "filename": "..." }`

### 7. **GET /health**
- **Purpose**: Health check endpoint
- **Response**: `{ "status": "healthy" }`

---

## Required Corrections in Android App

### 1. **ApiService.kt Interface**

Create/Update the Retrofit interface to match the backend:

```kotlin
package com.mvdown.api

import com.mvdown.models.DownloadRequest
import com.mvdown.models.DownloadResponse
import com.mvdown.models.FileInfo
import com.mvdown.models.FormatRequest
import com.mvdown.models.FormatResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("api/formats")
    suspend fun getFormats(@Body request: FormatRequest): Response<FormatResponse>
    
    @POST("api/download")
    suspend fun initiateDownload(@Body request: DownloadRequest): Response<DownloadResponse>
    
    @GET("files")
    suspend fun getFiles(): Response<List<FileInfo>>
    
    @DELETE("api/files/{filename}")
    suspend fun deleteFile(@Path("filename") filename: String): Response<Unit>
    
    @Streaming
    @GET("downloads/{filename}")
    suspend fun downloadFile(
        @Path("filename") filename: String,
        @Header("Range") range: String? = null
    ): Response<ResponseBody>
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
```

### 2. **Data Models (Format.kt and new models)**

Create the following data classes:

```kotlin
package com.mvdown.models

import com.google.gson.annotations.SerializedName

// Request Models
data class FormatRequest(
    val url: String
)

data class DownloadRequest(
    val url: String,
    @SerializedName("format_id")
    val formatId: String
)

// Response Models
data class FormatResponse(
    val title: String,
    val url: String,
    @SerializedName("video_formats")
    val videoFormats: List<VideoFormat>,
    @SerializedName("audio_formats")
    val audioFormats: List<AudioFormat>
)

data class VideoFormat(
    @SerializedName("format_id")
    val formatId: String,
    val ext: String?,
    val resolution: String?,
    val filesize: Long?
)

data class AudioFormat(
    @SerializedName("format_id")
    val formatId: String,
    val ext: String?,
    val abr: Double?,
    val filesize: Long?
)

data class DownloadResponse(
    @SerializedName("download_id")
    val downloadId: String,
    @SerializedName("websocket_url")
    val websocketUrl: String,
    val message: String
)

data class FileInfo(
    val name: String,
    val size: Long,
    val type: String // "video", "audio", or "others"
)

// WebSocket Progress Events
sealed class DownloadEvent {
    data class Progress(
        val status: String,
        val percent: String? = null,
        val downloadedBytes: Long? = null,
        val totalBytes: Long? = null,
        val eta: Int? = null,
        val speed: Double? = null,
        val message: String? = null
    ) : DownloadEvent()
    
    data class Done(
        val title: String?,
        val filename: String,
        val downloadUrl: String
    ) : DownloadEvent()
    
    data class Error(
        val error: String
    ) : DownloadEvent()
}
```

### 3. **ApiClient.kt Configuration**

Update your ApiClient to use the correct base URL:

```kotlin
package com.mvdown.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // Update this to your backend URL
    private const val BASE_URL = "http://YOUR_BACKEND_IP:8000/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: ApiService = retrofit.create(ApiService::class.java)
}
```

### 4. **WebSocket Implementation**

Create a WebSocket manager for download progress:

```kotlin
package com.mvdown.websocket

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mvdown.models.DownloadEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import okio.ByteString

class DownloadWebSocketManager(private val baseUrl: String) {
    
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val eventChannel = Channel<DownloadEvent>(Channel.BUFFERED)
    
    fun connect(downloadId: String): Flow<DownloadEvent> {
        val wsUrl = baseUrl.replace("http://", "ws://")
            .replace("https://", "wss://") + "ws/$downloadId"
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val event = json.get("event")?.asString
                    
                    when (event) {
                        "progress" -> {
                            eventChannel.trySend(DownloadEvent.Progress(
                                status = json.get("status")?.asString ?: "",
                                percent = json.get("percent")?.asString,
                                downloadedBytes = json.get("downloaded_bytes")?.asLong,
                                totalBytes = json.get("total_bytes")?.asLong,
                                eta = json.get("eta")?.asInt,
                                speed = json.get("speed")?.asDouble,
                                message = json.get("message")?.asString
                            ))
                        }
                        "done" -> {
                            eventChannel.trySend(DownloadEvent.Done(
                                title = json.get("title")?.asString,
                                filename = json.get("filename")?.asString ?: "",
                                downloadUrl = json.get("download_url")?.asString ?: ""
                            ))
                        }
                        "error" -> {
                            eventChannel.trySend(DownloadEvent.Error(
                                error = json.get("error")?.asString ?: "Unknown error"
                            ))
                        }
                    }
                } catch (e: Exception) {
                    eventChannel.trySend(DownloadEvent.Error(e.message ?: "Parse error"))
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                eventChannel.trySend(DownloadEvent.Error(t.message ?: "Connection failed"))
            }
        })
        
        return eventChannel.receiveAsFlow()
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}
```

### 5. **MainActivity.kt Corrections**

Update the MainActivity to use the correct request format:

```kotlin
private fun fetchFormats(url: String) {
    lifecycleScope.launch {
        try {
            // Create request object instead of passing URL directly
            val request = FormatRequest(url = url)
            val response = ApiClient.service.getFormats(request)
            
            if (response.isSuccessful && response.body() != null) {
                val formats = response.body()!!
                FormatBottomSheet.newInstance(formats)
                    .show(supportFragmentManager, "FormatBottomSheet")
            } else {
                Toast.makeText(this@MainActivity, 
                    "Error: ${response.errorBody()?.string() ?: "Unknown error"}", 
                    Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, 
                "Error: ${e.message ?: "Unknown error"}", 
                Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 6. **FormatBottomSheet Download Implementation**

When user selects a format to download:

```kotlin
private fun initiateDownload(url: String, formatId: String) {
    lifecycleScope.launch {
        try {
            val request = DownloadRequest(url = url, formatId = formatId)
            val response = ApiClient.service.initiateDownload(request)
            
            if (response.isSuccessful && response.body() != null) {
                val downloadResponse = response.body()!!
                
                // Connect to WebSocket for progress
                connectToWebSocket(downloadResponse.downloadId)
            } else {
                // Handle error
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }
}

private fun connectToWebSocket(downloadId: String) {
    val wsManager = DownloadWebSocketManager("http://YOUR_BACKEND_IP:8000/")
    
    lifecycleScope.launch {
        wsManager.connect(downloadId).collect { event ->
            when (event) {
                is DownloadEvent.Progress -> {
                    // Update progress UI
                    updateProgress(event.percent, event.status)
                }
                is DownloadEvent.Done -> {
                    // Download complete, start file download
                    downloadFile(event.filename)
                }
                is DownloadEvent.Error -> {
                    // Show error
                    showError(event.error)
                }
            }
        }
    }
}
```

### 7. **Required Dependencies (build.gradle)**

Add these dependencies:

```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    
    // Gson
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

---

## Critical Points to Remember

1. **Base URL**: Update `YOUR_BACKEND_IP` with your actual backend server IP address
2. **POST Requests**: All POST endpoints require JSON request bodies, not query parameters
3. **WebSocket**: The backend uses WebSocket for real-time progress, you need to implement WebSocket client
4. **File Download**: After getting "done" event, download the file from `/downloads/{filename}`
5. **Error Handling**: Backend returns specific error messages in response body
6. **Spotify Support**: Backend has special handling for Spotify URLs
7. **Range Requests**: The `/downloads/` endpoint supports resumable downloads via Range header

---

## Testing Checklist

- [ ] Test format fetching with YouTube URL
- [ ] Test format fetching with Spotify URL
- [ ] Test download initiation
- [ ] Test WebSocket connection and progress updates
- [ ] Test file download after completion
- [ ] Test file list retrieval
- [ ] Test file deletion
- [ ] Test error scenarios (invalid URL, network error, etc.)
- [ ] Test resumable downloads with Range header
