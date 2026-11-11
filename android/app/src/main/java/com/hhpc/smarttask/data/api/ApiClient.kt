package com.hhpc.smarttask.data.api

import android.content.Context
import com.hhpc.smarttask.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Android Emulator connects to Host localhost via 10.0.2.2:8085/api/
    private const val BASE_URL = "http://10.0.2.2:8085/api/"

    private var retrofit: Retrofit? = null

    fun getRetrofit(context: Context): Retrofit {
        if (retrofit == null) {
            val tokenManager = TokenManager(context.applicationContext)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getAuthApi(context: Context): AuthApi = getRetrofit(context).create(AuthApi::class.java)
    fun getTaskApi(context: Context): TaskApi = getRetrofit(context).create(TaskApi::class.java)
    fun getWorkspaceApi(context: Context): WorkspaceApi = getRetrofit(context).create(WorkspaceApi::class.java)
    fun getCategoryApi(context: Context): CategoryApi = getRetrofit(context).create(CategoryApi::class.java)
    fun getAiApi(context: Context): AiApi = getRetrofit(context).create(AiApi::class.java)
    fun getProductivityApi(context: Context): ProductivityApi = getRetrofit(context).create(ProductivityApi::class.java)
    fun getUserPreferenceApi(context: Context): UserPreferenceApi = getRetrofit(context).create(UserPreferenceApi::class.java)
}
