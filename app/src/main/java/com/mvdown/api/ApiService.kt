package com.mvdown.api



import com.mvdown.Format

import retrofit2.Response

import retrofit2.http.GETimport com.mvdown.Formatimport com.mvdown.models.Format

import retrofit2.http.Query

import retrofit2.Responseimport com.mvdown.models.MediaInfo

interface ApiService {

    @GET("/formats")import retrofit2.http.GETimport retrofit2.Response

    suspend fun getFormats(@Query("url") url: String): Response<List<Format>>

}import retrofit2.http.Queryimport retrofit2.http.GET

import retrofit2.http.POST

interface ApiService {import retrofit2.http.Query

    @GET("/formats")

    suspend fun getFormats(@Query("url") url: String): Response<List<Format>>interface ApiService {

}    @GET("info")
    suspend fun getMediaInfo(@Query("url") url: String): Response<MediaInfo>
    
    @GET("formats")
    suspend fun getFormats(@Query("url") url: String): Response<List<Format>>
    
    @POST("download")
    suspend fun getDownloadLink(
        @Query("url") url: String,
        @Query("format") format: String
    ): Response<String>
}