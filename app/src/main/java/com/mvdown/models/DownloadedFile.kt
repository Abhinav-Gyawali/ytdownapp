package com.mvdown.models

data class DownloadedFile(
    val title: String,
    val filePath: String,
    val downloadDate: Long,
    val size: Long
)