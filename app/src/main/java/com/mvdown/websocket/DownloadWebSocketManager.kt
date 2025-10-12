package com.mvdown.websocket

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mvdown.models.DownloadEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class DownloadWebSocketManager(private val baseUrl: String) {
    
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
        .pingInterval(30, TimeUnit.SECONDS) // Keep connection alive
        .build()
        
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val eventChannel = Channel<DownloadEvent>(Channel.BUFFERED)
    
    init {
        println("🔌 WebSocketManager initialized")
    }
    
    fun connect(downloadId: String): Flow<DownloadEvent> {
        // First disconnect any existing connection
        disconnect()
        
        // Build WebSocket URL
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
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}
