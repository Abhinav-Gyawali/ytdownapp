package com.mvdown.models

import com.google.gson.annotations.SerializedName

// Request Models
data class FormatRequest(
    val url: String
)

data class DownloadRequest(
    val url: String,
    @SerializedName("format_id")
    val formatId: String
)

// Response Models
data class FormatResponse(
    val title: String,
    val url: String,
    @SerializedName("video_formats")
    val videoFormats: List<VideoFormat>,
    @SerializedName("audio_formats")
    val audioFormats: List<AudioFormat>
)

data class VideoFormat(
    @SerializedName("format_id")
    val formatId: String,
    val ext: String?,
    val resolution: String?,
    val filesize: Long?
)

data class AudioFormat(
    @SerializedName("format_id")
    val formatId: String,
    val ext: String?,
    val abr: Double?,
    val filesize: Long?
)

data class DownloadResponse(
    @SerializedName("download_id")
    val downloadId: String,
    @SerializedName("websocket_url")
    val websocketUrl: String,
    val message: String
)

data class FileInfo(
    val name: String,
    val size: Long,
    val type: String // "video", "audio", or "others"
)

// WebSocket Progress Events
sealed class DownloadEvent {
    data class Progress(
        val status: String,
        val percent: String? = null,
        val downloadedBytes: Long? = null,
        val totalBytes: Long? = null,
        val eta: Int? = null,
        val speed: Double? = null,
        val message: String? = null
    ) : DownloadEvent()
    
    data class Done(
        val title: String?,
        val filename: String,
        val downloadUrl: String
    ) : DownloadEvent()
    
    data class Error(
        val error: String
    ) : DownloadEvent()
}
