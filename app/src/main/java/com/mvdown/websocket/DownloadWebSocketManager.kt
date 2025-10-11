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
