package com.mvdown.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mvdown.api.ApiClient
import com.mvdown.models.DownloadEvent
import com.mvdown.models.DownloadRequest
import com.mvdown.sse.SSEManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class DownloadManager(private val context: Context) {
    
    private val _downloadEvent = MutableLiveData<DownloadEvent>()
    val downloadEvent: LiveData<DownloadEvent> = _downloadEvent
    
    private var sseManager: SSEManager? = null
    
    companion object {
        private const val TAG = "DownloadManager"
    }
    
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
                
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val downloadResponse = response.body()!!
                    val downloadId = downloadResponse.downloadId
                    
                    Log.d(TAG, "✅ Download initiated! ID: $downloadId")
                    
                    // Step 2: Connect SSE with the download_id
                    sseManager = SSEManager(downloadId)
                    sseManager?.startListening()
                        ?.catch { error ->
                            Log.e(TAG, "SSE error: ${error.message}", error)
                            _downloadEvent.postValue(
                                DownloadEvent.Error(error.message ?: "Unknown error")
                            )
                        }
                        ?.collect { event ->
                            _downloadEvent.postValue(event)
                        }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "❌ Download initiation failed: ${response.code()} - $errorBody")
                    _downloadEvent.postValue(
                        DownloadEvent.Error("Failed to start download: ${response.code()} - $errorBody")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Download error: ${e.message}", e)
                _downloadEvent.postValue(
                    DownloadEvent.Error(e.message ?: "Unknown error")
                )
            }
        }
    }
    
    fun disconnect() {
        sseManager?.close()
        sseManager = null
    }
}