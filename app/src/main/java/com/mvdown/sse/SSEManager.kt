package com.mvdown.sse

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

class SSEManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var eventSource: EventSource? = null

    fun startListening(downloadId: String): Flow<SSEEvent> {
        val url = "${ApiClient.BASE_URL.removeSuffix("/")}/api/progress/$downloadId"

        val channel = Channel<SSEEvent>(Channel.BUFFERED)

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/event-stream")
            .build()

        eventSource = EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d("SSEManager", "SSE connection opened")
                channel.trySend(SSEEvent.Connected)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.d("SSEManager", "SSE Event received: $data")
                try {
                    val json = JsonParser.parseString(data).asJsonObject

                    val status = json.get("status")?.asString ?: "unknown"
                    val progress = json.get("progress")?.asInt ?: 0
                    val downloaded = json.get("downloaded_bytes")?.asLong ?: 0L
                    val total = json.get("total_bytes")?.asLong ?: 0L
                    val speed = json.get("speed")?.asString ?: ""

                    when (status) {
                        "done" -> channel.trySend(SSEEvent.Done(json))
                        "error" -> channel.trySend(SSEEvent.Error(json))
                        else -> channel.trySend(
                            SSEEvent.Progress(
                                progress = progress,
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                speed = speed
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SSEManager", "Error parsing SSE data", e)
                    channel.trySend(SSEEvent.Error(null))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("SSEManager", "SSE connection closed")
                channel.trySend(SSEEvent.Closed)
                channel.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("SSEManager", "SSE connection error", t)
                channel.trySend(SSEEvent.Error(null))
                channel.close()
            }
        })

        return channel.receiveAsFlow()
    }

    fun close() {
        eventSource?.cancel()
    }
}

sealed class SSEEvent {
    data class Progress(val progress: Int, val downloadedBytes: Long, val totalBytes: Long, val speed: String) : SSEEvent()
    data class Done(val data: com.google.gson.JsonObject?) : SSEEvent()
    data class Error(val data: com.google.gson.JsonObject?) : SSEEvent()
    object Connected : SSEEvent()
    object Closed : SSEEvent()
}
