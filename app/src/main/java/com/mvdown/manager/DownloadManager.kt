package com.mvdown.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mvdown.api.ApiClient
import com.mvdown.models.DownloadEvent
import com.mvdown.models.DownloadRequest
import com.mvdown.sse.SSEManager
import com.mvdown.api.ApiClient

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
    _downloadEvent.postValue(DownloadEvent.Progress("starting", "Initiating download..."))

    scope.launch(Dispatchers.IO) {
        try {
            val request = DownloadRequest(url, formatId)
            val response = ApiClient.service.initiateDownload(request)

            if (response.isSuccessful && response.body() != null) {
                val downloadId = response.body()!!.downloadId
                Log.d(TAG, "Download ID: $downloadId")

                // Ensure previous SSE is closed
                disconnect()
                sseManager = SSEManager()

                sseManager?.startListening(downloadId)
                    ?.catch { error ->
                        Log.e(TAG, "SSE error: ${error.message}", error)
                        _downloadEvent.postValue(DownloadEvent.Error(error.message ?: "Unknown error"))
                    }
                    ?.collect { event ->
                        _downloadEvent.postValue(event)
                    }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                _downloadEvent.postValue(DownloadEvent.Error("Failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            _downloadEvent.postValue(DownloadEvent.Error(e.message ?: "Unknown error"))
        }
    }
}
    
    fun disconnect() {
        sseManager?.close()
        sseManager = null
    }
}
