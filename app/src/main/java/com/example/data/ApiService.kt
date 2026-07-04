package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @GET("cases")
    suspend fun getCases(): List<CaseEntity>

    @GET("open-cases")
    suspend fun getOpenCases(): List<CaseEntity>

    @GET("suspects")
    suspend fun getSuspects(): List<SuspectEntity>

    @GET("warrants")
    suspend fun getWarrants(): List<SuspectEntity>

    @POST("cases")
    suspend fun addCase(@Body caseEntity: CaseEntity): CaseEntity

    @POST("assign-detective")
    suspend fun assignDetective(@Body request: AssignDetectiveRequest): SimpleMessageResponse

    @GET("suspects/{id}/risk")
    suspend fun getSuspectRisk(@Path("id") id: Int): RiskScoreResponse

    companion object {
        fun create(baseUrl: String): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ApiService::class.java)
        }
    }
}
