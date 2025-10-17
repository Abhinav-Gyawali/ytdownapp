package com.mvdown.network

import com.mvdown.model.*
import retrofit2.http.*

interface ApiService {
    
    @POST("/api/formats")
    suspend fun getFormats(@Body request: FormatRequest): FormatResponse
    
    @POST("/api/download")
    suspend fun initiateDownload(@Body request: DownloadRequest): DownloadResponse
    
    @GET("/api/files")
    suspend fun getFiles(): List<FileItem>
    
    @DELETE("/api/files/{filename}")
    suspend fun deleteFile(@Path("filename") filename: String): DeleteResponse
    
    @DELETE("/api/files")
    suspend fun deleteAllFiles(): DeleteAllResponse
    
    @GET("/health")
    suspend fun getHealth(): HealthResponse
}