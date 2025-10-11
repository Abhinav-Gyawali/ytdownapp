package com.mvdown.api



import okhttp3.OkHttpClient

import okhttp3.logging.HttpLoggingInterceptor

import retrofit2.Retrofitimport okhttp3.OkHttpClientimport okhttp3.OkHttpClient

import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.logging.HttpLoggingInterceptorimport okhttp3.logging.HttpLoggingInterceptor

object ApiClient {

    private const val BASE_URL = "https://ytdownbackend.onrender.com"import retrofit2.Retrofitimport retrofit2.Retrofit



    private val okHttpClient = OkHttpClient.Builder()import retrofit2.converter.gson.GsonConverterFactoryimport retrofit2.converter.gson.GsonConverterFactory

        .addInterceptor(HttpLoggingInterceptor().apply {

            level = HttpLoggingInterceptor.Level.BODYimport java.util.concurrent.TimeUnit

        })

        .build()object ApiClient {



    val service: ApiService by lazy {    private const val BASE_URL = "https://ytdownbackend.onrender.com"object ApiClient {

        Retrofit.Builder()

            .baseUrl(BASE_URL)    private const val BASE_URL = "https://ytdownbackend.onrender.com"

            .client(okHttpClient)

            .addConverterFactory(GsonConverterFactory.create())    private val okHttpClient = OkHttpClient.Builder()    

            .build()

            .create(ApiService::class.java)        .addInterceptor(HttpLoggingInterceptor().apply {    private val okHttpClient = OkHttpClient.Builder()

    }

}            level = HttpLoggingInterceptor.Level.BODY        .addInterceptor(HttpLoggingInterceptor().apply {

        })            level = HttpLoggingInterceptor.Level.BODY

        .build()        })

        .connectTimeout(30, TimeUnit.SECONDS)

    val service: ApiService by lazy {        .readTimeout(30, TimeUnit.SECONDS)

        Retrofit.Builder()        .writeTimeout(30, TimeUnit.SECONDS)

            .baseUrl(BASE_URL)        .build()

            .client(okHttpClient)        

            .addConverterFactory(GsonConverterFactory.create())    private val retrofit = Retrofit.Builder()

            .build()        .baseUrl(BASE_URL)

            .create(ApiService::class.java)        .client(okHttpClient)

    }        .addConverterFactory(GsonConverterFactory.create())

}        .build()
        
    val api: ApiService = retrofit.create(ApiService::class.java)
}