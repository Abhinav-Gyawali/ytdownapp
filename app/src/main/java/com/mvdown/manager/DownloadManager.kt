package com.mvdown.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mvdown.api.ApiClient
import com.mvdown.models.DownloadEvent
import com.mvdown.models.DownloadRequest
import com.mvdown.sse.SSEEvent
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
        _downloadEvent.postValue(DownloadEvent.Progress("starting", message = "Initiating download..."))

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
                        ?.collect { sseEvent ->
                            // Convert SSEEvent to DownloadEvent
                            val downloadEvent = convertSSEEventToDownloadEvent(sseEvent)
                            _downloadEvent.postValue(downloadEvent)
                        }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _downloadEvent.postValue(DownloadEvent.Error("Failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                _downloadEvent.postValue(DownloadEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }
    
    private fun convertSSEEventToDownloadEvent(sseEvent: SSEEvent): DownloadEvent {
        return when (sseEvent) {
            is SSEEvent.Connected -> {
                DownloadEvent.Progress("connected", message = "Connected to server")
            }
            
            is SSEEvent.Progress -> {
                val percentStr = if (sseEvent.totalBytes > 0) {
                    val percent = (sseEvent.downloadedBytes * 100.0 / sseEvent.totalBytes).toInt()
                    "$percent%"
                } else {
                    "${sseEvent.progress}%"
                }
                
                DownloadEvent.Progress(
                    status = "downloading",
                    percent = percentStr,
                    downloadedBytes = sseEvent.downloadedBytes,
                    totalBytes = sseEvent.totalBytes,
                    speed = parseSpeed(sseEvent.speed)
                )
            }
            
            is SSEEvent.Done -> {
                val data = sseEvent.data
                val filename = data?.get("filename")?.asString ?: "unknown"
                val title = data?.get("title")?.asString
                val downloadUrl = data?.get("download_url")?.asString ?: ""
                
                DownloadEvent.Done(
                    title = title,
                    filename = filename,
                    downloadUrl = downloadUrl
                )
            }
            
            is SSEEvent.Error -> {
                val errorMessage = sseEvent.data?.get("error")?.asString ?: "Unknown error occurred"
                DownloadEvent.Error(errorMessage)
            }
            
            is SSEEvent.Closed -> {
                DownloadEvent.Progress("closed", message = "Connection closed")
            }
        }
    }
    
    private fun parseSpeed(speedStr: String): Double? {
        if (speedStr.isBlank()) return null
        
        return try {
            // Remove any non-numeric characters except decimal point
            val numericStr = speedStr.replace(Regex("[^0-9.]"), "")
            numericStr.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    fun disconnect() {
        sseManager?.close()
        sseManager = null
    }
}
