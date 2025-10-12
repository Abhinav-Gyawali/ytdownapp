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
import kotlinx.coroutines.flow.collect

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
                
                println("Response code: ${response.code()}")
                println("Response body: ${response.body()}")
                
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
                    println("❌ Download initiation failed: ${response.code()} - $errorBody")
                    _downloadEvent.postValue(
                        DownloadEvent.Error("Failed to start download: ${response.code()} - $errorBody")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Download error: ${e.message}")
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