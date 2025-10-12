package com.mvdown.sse

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mvdown.api.ApiClient
import com.mvdown.models.DownloadEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class SSEManager(private val downloadId: String) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(0, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .build()
    
    private var eventSource: EventSource? = null
    private val gson = Gson()
    private val eventChannel = Channel<DownloadEvent>(Channel.BUFFERED)
    
    fun startListening(): Flow<DownloadEvent> {
        val sseUrl = "${ApiClient.BASE_URL.removeSuffix("/")}/api/progress/$downloadId"
        
        Log.d(TAG, "🔌 Connecting to SSE: $sseUrl")
        
        val request = Request.Builder()
            .url(sseUrl)
            .header("Accept", "text/event-stream")
            .build()
        
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d("SSEManager", "✅ SSE Connected")
            }
            
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                Log.d("SSEManager", "📩 SSE Event: $data")
                try {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    val event = json.get("event")?.asString
                    
                    when (event) {
                        "connected" -> {
                            Log.d("SSEManager", "🤝 Connection acknowledged")
                        }
                        "progress" -> {
                            val percentStr = json.get("percent")?.asString ?: "0%"
                            eventChannel.trySend(DownloadEvent.Progress(
                                status = json.get("status")?.asString ?: "downloading",
                                percent = percentStr,
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
                    Log.e("SSEManager", "❌ Parse error: ${e.message}")
                    eventChannel.trySend(DownloadEvent.Error(e.message ?: "Parse error"))
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                Log.d("SSEManager", "🔌 SSE Closed")
            }
            
            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                Log.e("SSEManager", "❌ SSE Failed: ${t?.message}")
                eventChannel.trySend(DownloadEvent.Error(t?.message ?: "Connection failed"))
            }
        }
        
        eventSource = EventSources.createFactory(client).newEventSource(request, listener)
        
        return eventChannel.receiveAsFlow()
    }
    
    fun close() {
        eventSource?.cancel()
        eventSource = null
        eventChannel.close()
    }
    
    companion object {
        private const val TAG = "SSEManager"
    }
}