package com.cs407.myapplication.network

import com.cs407.myapplication.network.DietPlanApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object DietPlanApiClient {

    private const val BASE_URL = "https://calculating-belle-predestinately.ngrok-free.dev/"


    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.SECONDS)   // 连接超时
            .readTimeout(3000, TimeUnit.SECONDS)      // 读超时（等待服务器返回）
            .writeTimeout(3000, TimeUnit.SECONDS)     // 写超时
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    val service: DietPlanApiService by lazy {
        retrofit.create(DietPlanApiService::class.java)
    }
}