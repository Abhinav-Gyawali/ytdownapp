package com.mvdown.api

import com.mvdown.models.DownloadRequest
import com.mvdown.models.DownloadResponse
import com.mvdown.models.FileInfo
import com.mvdown.models.FormatRequest
import com.mvdown.models.FormatResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("api/formats")
    suspend fun getFormats(@Body request: FormatRequest): Response<FormatResponse>
    
    @POST("api/download")
    suspend fun initiateDownload(@Body request: DownloadRequest): Response<DownloadResponse>
    
    @GET("files")
    suspend fun getFiles(): Response<List<FileInfo>>
    
    @DELETE("api/files/{filename}")
    suspend fun deleteFile(@Path("filename") filename: String): Response<Unit>
    
    @Streaming
    @GET("downloads/{filename}")
    suspend fun downloadFile(
        @Path("filename") filename: String,
        @Header("Range") range: String? = null
    ): Response<ResponseBody>
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
