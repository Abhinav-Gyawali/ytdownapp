# 🔧 Fix WebSocket 403 Forbidden Error

## 🚨 Problem Identified

**Server Logs:**
```
INFO: ('10.214.148.122', 0) - "WebSocket /ws" 403
INFO: connection rejected (403 Forbidden)
INFO: connection closed
```

**Root Cause:** The app is connecting to `/ws` instead of `/ws/{download_id}`

The server expects: `/ws/abc123-def456`  
But app is sending: `/ws` ❌

---

## 📋 Why This Happens

The WebSocket endpoint requires a download_id in the path:
```python
@app.websocket("/ws/{download_id}")  # ← Requires download_id
async def websocket_endpoint(websocket: WebSocket, download_id: str):
```

The app must:
1. ✅ First call `POST /api/download` → Get `download_id`
2. ✅ Then connect to `/ws/{download_id}` with that ID

---

## 🛠️ Complete Fix

### Fix 1: Update DownloadManager.kt

**File:** `app/src/main/java/com/mvdown/manager/DownloadManager.kt`

**Replace entire file with:**

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
        // Send initial "processing" event
        _downloadEvent.postValue(
            DownloadEvent.Progress(
                status = "starting",
                message = "Initiating download..."
            )
        )
        
        scope.launch(Dispatchers.IO) {
            try {
                // Step 1: Initiate download and get download_id
                val request = DownloadRequest(url, formatId)
                val response = ApiClient.service.initiateDownload(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val downloadResponse = response.body()!!
                    val downloadId = downloadResponse.downloadId
                    
                    // Log for debugging
                    println("✅ Download initiated! ID: $downloadId")
                    
                    // Step 2: Connect WebSocket with the download_id
                    wsManager.connect(downloadId).collect { event ->
                        _downloadEvent.postValue(event)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _downloadEvent.postValue(
                        DownloadEvent.Error("Failed to start download: ${response.code()} - $errorBody")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

**Key Changes:**
- ✅ Added initial "starting" event for immediate UI feedback
- ✅ Proper error handling with error body
- ✅ Debug logging to track download_id
- ✅ Correct flow: API call first, then WebSocket

---

### Fix 2: Verify WebSocket URL Building

**File:** `app/src/main/java/com/mvdown/websocket/DownloadWebSocketManager.kt`

**Update the `connect` function:**

```kotlin
fun connect(downloadId: String): Flow<DownloadEvent> {
    // Ensure clean URL building with proper slash handling
    val wsUrl = baseUrl
        .replace("http://", "ws://")
        .replace("https://", "wss://")
        .removeSuffix("/") + "/ws/$downloadId"
    
    // Debug logging
    println("🔌 Connecting to WebSocket: $wsUrl")
    println("📝 Download ID: $downloadId")
    
    val request = Request.Builder()
        .url(wsUrl)
        .build()
    
    webSocket = client.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("✅ WebSocket connected successfully!")
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            println("📩 WebSocket message: $text")
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
                println("❌ Error parsing message: ${e.message}")
                eventChannel.trySend(DownloadEvent.Error(e.message ?: "Parse error"))
            }
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("❌ WebSocket failed: ${t.message}")
            println("❌ Response: ${response?.code} ${response?.message}")
            eventChannel.trySend(DownloadEvent.Error(t.message ?: "Connection failed"))
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("🔌 WebSocket closed: $code - $reason")
        }
    })
    
    return eventChannel.receiveAsFlow()
}
```

**Key Changes:**
- ✅ Clean URL building with `removeSuffix("/")`
- ✅ Extensive debug logging
- ✅ Added `onOpen` callback
- ✅ Better error logging

---

### Fix 3: Add Debug Logging to FormatBottomSheet

**File:** `app/src/main/java/com/mvdown/ui/bottom_sheets/FormatBottomSheet.kt`

**Update the `startDownload` function:**

```kotlin
private fun startDownload(format: VideoFormat) {
    println("=" * 50)
    println("📥 STARTING DOWNLOAD")
    println("📍 URL: $url")
    println("🎬 Format ID: ${format.formatId}")
    println("📐 Resolution: ${format.resolution}")
    println("=" * 50)
    
    dismiss()
    
    progressDialog = DownloadProgressDialog(requireContext()).apply {
        show()
    }
    
    downloadManager.startDownload(url, format.formatId, lifecycleScope)
}
```

---

## 🧪 Testing & Verification

### Step 1: Check Logcat Output

After clicking download, you should see in Logcat:

```
==================================================
📥 STARTING DOWNLOAD
📍 URL: https://youtube.com/watch?v=dQw4w9WgXcQ
🎬 Format ID: 137
📐 Resolution: 1080p
==================================================
✅ Download initiated! ID: abc123-def456-789
🔌 Connecting to WebSocket: ws://ytdownbackend.onrender.com/ws/abc123-def456-789
📝 Download ID: abc123-def456-789
✅ WebSocket connected successfully!
📩 WebSocket message: {"event":"progress","status":"starting","message":"Starting download..."}
```

### Step 2: Check Server Logs

Server should now show:

```
✅ INFO: POST /api/download - 200 OK
✅ INFO: WebSocket /ws/abc123-def456-789 - ACCEPTED
✅ INFO: Progress event sent
```

Instead of:

```
❌ INFO: WebSocket /ws - 403 Forbidden
```

---

## 🔍 Debugging Steps

### If Still Getting 403:

1. **Check if download_id is being retrieved:**
```kotlin
// Add this in DownloadManager after API call
println("Response code: ${response.code()}")
println("Response body: ${response.body()}")
```

2. **Verify BASE_URL format:**
```kotlin
// In ApiClient.kt, print the URL
println("🌐 BASE_URL: ${BASE_URL}")
// Should be: https://ytdownbackend.onrender.com/
// NOT: https://ytdownbackend.onrender.com (without trailing slash)
```

3. **Check WebSocket URL construction:**
```kotlin
// The constructed URL should look like:
// ws://ytdownbackend.onrender.com/ws/abc123-def456
// NOT: ws://ytdownbackend.onrender.comws/abc123-def456
// NOT: ws://ytdownbackend.onrender.com//ws/abc123-def456
```

---

## 📊 Correct Flow Diagram

```
User clicks format
       ↓
┌──────────────────────┐
│ POST /api/download   │ ← App sends URL + format_id
└──────────────────────┘
       ↓
┌──────────────────────┐
│ Server returns:      │
│ {                    │
│   download_id: "abc" │
│   websocket_url: ... │
│ }                    │
└──────────────────────┘
       ↓
┌──────────────────────┐
│ Connect WebSocket    │ ← App connects with download_id
│ WS /ws/abc           │
└──────────────────────┘
       ↓
┌──────────────────────┐
│ Server ACCEPTS (101) │ ← Success!
└──────────────────────┘
       ↓
┌──────────────────────┐
│ Progress events flow │
└──────────────────────┘
```

---

## 🎯 Expected Results After Fix

### Android Logcat:
```
✅ Download initiated! ID: {uuid}
✅ WebSocket connected successfully!
📩 WebSocket message: {"event":"progress",...}
📩 WebSocket message: {"event":"done",...}
```

### Server Logs:
```
✅ POST /api/download HTTP/1.1" 200 OK
✅ WebSocket /ws/{uuid} - ACCEPTED
✅ Download progress: 45%
✅ Download complete
```

### UI Behavior:
- ✅ Progress dialog shows immediately
- ✅ "Processing your request..." message appears
- ✅ Progress percentage updates in real-time
- ✅ Speed and ETA display
- ✅ Completion toast shows with filename

---

## 💡 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Still getting 403 | Check that download_id is not null/empty |
| No progress updates | Verify WebSocket onMessage is being called |
| App crashes | Check all LiveData observers are on main thread |
| URL malformed | Use `.removeSuffix("/")` before adding path |

---

## ✅ Verification Checklist

- [ ] DownloadManager.kt updated
- [ ] DownloadWebSocketManager.kt updated  
- [ ] Debug logging added
- [ ] BASE_URL is public and correct
- [ ] Clean & rebuild project
- [ ] Test with real YouTube URL
- [ ] Check Logcat for logs
- [ ] Verify server accepts WebSocket
- [ ] Progress dialog shows and updates
- [ ] Download completes successfully

---

**After applying all fixes, the WebSocket 403 error should be resolved!** 🎉