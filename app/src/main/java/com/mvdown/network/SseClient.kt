package com.mvdown.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.BufferedReader
import java.io.InputStreamReader

class SseClient {
    
    private var client: OkHttpClient? = null
    private var call: Call? = null
    
    suspend fun connect(downloadId: String, onProgress: (Map<String, Any>) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                client = OkHttpClient.Builder()
                    .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val url = "${ApiClient.BASE_URL}/api/download/progress/$downloadId"
                val request = Request.Builder().url(url).build()
                
                call = client!!.newCall(request)
                val response = call!!.execute()
                
                if (response.isSuccessful) {
                    val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        if (line?.startsWith("data: ") == true) {
                            val data = line!!.substring(6)
                            try {
                                val progress = Gson().fromJson(data, Map::class.java) as Map<String, Any>
                                onProgress(progress)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun disconnect() {
        call?.cancel()
        client = null
    }
}