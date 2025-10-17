package com.mvdown.model

import com.google.gson.annotations.SerializedName

data class FormatRequest(
    val url: String
)

data class DownloadRequest(
    val url: String,
    @SerializedName("format_id") val formatId: String
)

data class FormatResponse(
    val title: String,
    val url: String,
    @SerializedName("video_formats") val videoFormats: List<Format>?,
    @SerializedName("audio_formats") val audioFormats: List<Format>?,
    @SerializedName("is_playlist") val isPlaylist: Boolean?
)

data class Format(
    @SerializedName("format_id") val formatId: String,
    val ext: String,
    val resolution: String?,
    val abr: Double?,
    val filesize: Long?,
    @SerializedName("format_note") val formatNote: String?
)

data class DownloadResponse(
    @SerializedName("download_id") val downloadId: String,
    @SerializedName("sse_url") val sseUrl: String,
    val message: String
)

data class FileItem(
    val name: String,
    val size: Long,
    val type: String,
    val mimetype: String,
    @SerializedName("download_url") val downloadUrl: String,
    val extension: String
)

data class DeleteResponse(
    val success: Boolean,
    val message: String,
    val filename: String,
    @SerializedName("freed_bytes") val freedBytes: Long
)

data class DeleteAllResponse(
    val success: Boolean,
    @SerializedName("deleted_count") val deletedCount: Int,
    @SerializedName("freed_bytes") val freedBytes: Long
)

data class HealthResponse(
    val status: String,
    val cookies: String,
    @SerializedName("download_dir") val downloadDir: String,
    @SerializedName("free_space_gb") val freeSpaceGb: Double,
    @SerializedName("files_count") val filesCount: Int,
    @SerializedName("active_downloads") val activeDownloads: Int
)
