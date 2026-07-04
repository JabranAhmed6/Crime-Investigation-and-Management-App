package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object GeminiService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Moshi classes for Gemini REST API
    data class GeminiRequest(
        val contents: List<GeminiContent>,
        val systemInstruction: GeminiContent? = null
    )

    data class GeminiContent(
        val parts: List<GeminiPart>
    )

    data class GeminiPart(
        val text: String
    )

    data class GeminiResponse(
        val candidates: List<GeminiCandidate>?
    )

    data class GeminiCandidate(
        val content: GeminiContent?
    )

    suspend fun analyzeCase(
        caseType: String,
        status: String,
        jurisdiction: String,
        suspects: String,
        evidence: String,
        timeline: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Analysis Error: Gemini API Key is not set in AI Studio Secrets. Please check your configuration."
        }

        val prompt = """
            Case Profile:
            - Crime Type: $caseType
            - Current Status: $status
            - Jurisdiction: $jurisdiction
            
            Associated Suspects:
            $suspects
            
            Evidence Gathered:
            $evidence
            
            Case Timeline & Events:
            $timeline
            
            Please perform an expert criminal investigation analysis. Structure your response into:
            1. 🔍 INVESTIGATION ASSESSMENT: Critical evaluation of the case and immediate red flags.
            2. 🧬 EVIDENCE INSIGHTS: Forensic or digital review of the evidence and potential lead value.
            3. 👥 SUSPECT PROFILING & RISK: Risk analysis of repeat offenders and potential motives.
            4. 🚀 ACTIONABLE NEXT STEPS: Top 3 concrete next actions for the detective team.
            
            Keep your tone professional, tactical, and direct, suitable for an agency briefing.
        """.trimIndent()

        val systemPrompt = "You are Sherlock, an expert AI Crime Analyst and consulting forensic detective assisting law enforcement agencies globally."

        val requestObj = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
        )

        try {
            val requestAdapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequestBody = requestAdapter.toJson(requestObj)

            val requestBody = jsonRequestBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "API Call Failed: HTTP ${response.code} - ${response.message}"
            }

            val responseBodyString = response.body?.string() ?: return@withContext "Error: Empty response from AI service."
            
            val responseAdapter = moshi.adapter(GeminiResponse::class.java)
            val geminiResponse = responseAdapter.fromJson(responseBodyString)
            
            val text = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                text
            } else {
                "Failed to parse analysis report. Please retry."
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Error calling Gemini API: ${e.message}", e)
            "Error analyzing case: ${e.localizedMessage}"
        }
    }
}
