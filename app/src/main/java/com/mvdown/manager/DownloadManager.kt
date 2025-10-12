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
                        DownloadEvent.Error(response.errorBody()?.string() ?: "Unknown error")
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