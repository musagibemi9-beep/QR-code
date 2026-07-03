package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Data Classes for Retrofit ---
@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String = "1:1",
    val imageSize: String = "1K"
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiRestService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiRestService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiRestService::class.java)
    }
}

object GeminiAiService {

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    suspend fun searchSmartLink(query: String): Pair<String, String> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair("https://google.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8"), "Demo Search Result: Configure GEMINI_API_KEY for live grounding.")
        }
        val prompt = """
            You are a Smart Link Finder for a QR Code Generator.
            User Query: "$query"
            Find the exact official verified website URL, WhatsApp link, or registration link for this brand/service/event using Google Search grounding.
            Return a short summary response in two parts:
            1. On the very first line, print ONLY the direct URL starting with https://
            2. On the next lines, provide a 1-sentence explanation of what this link is.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            tools = listOf(buildJsonObject {
                putJsonObject("googleSearch") {}
            }),
            generationConfig = GenerationConfig(temperature = 0.2f)
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val lines = text.lines().filter { it.isNotBlank() }
            val firstLine = lines.firstOrNull() ?: ""
            val url = if (firstLine.startsWith("http")) firstLine else {
                // regex extract url
                val match = Regex("https?://[^\\s]+").find(text)
                match?.value ?: "https://google.com"
            }
            val summary = if (lines.size > 1) lines.drop(1).joinToString(" ") else "Verified official link via Google Search."
            Pair(url, summary)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("https://google.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8"), "Search Error: ${e.message}")
        }
    }

    suspend fun searchSmartLocation(placeQuery: String): Triple<String, String, String> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Triple("37.4220", "-122.0841", "Demo Location: Googleplex Mountain View (Add GEMINI_API_KEY for live Google Maps grounding)")
        }
        val prompt = """
            You are a Smart Location Finder for a QR Code Generator.
            Place Query: "$placeQuery"
            Find the exact geographical latitude, longitude, and formatted address for this place or business using Google Maps grounding.
            Return ONLY a JSON formatted string or plain text with:
            Line 1: LATITUDE (number only, e.g. 48.8584)
            Line 2: LONGITUDE (number only, e.g. 2.2945)
            Line 3: Formatted Address name
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            tools = listOf(buildJsonObject {
                putJsonObject("googleMaps") {}
            }),
            generationConfig = GenerationConfig(temperature = 0.2f)
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val lines = text.lines().filter { it.isNotBlank() }
            val lat = lines.getOrNull(0)?.replace(Regex("[^0-9.-]"), "") ?: "0.0"
            val lng = lines.getOrNull(1)?.replace(Regex("[^0-9.-]"), "") ?: "0.0"
            val address = lines.getOrNull(2) ?: placeQuery
            Triple(lat, lng, address)
        } catch (e: Exception) {
            e.printStackTrace()
            Triple("40.7128", "-74.0060", "Error finding location: ${e.message}. Defaulting to New York City.")
        }
    }

    suspend fun generateQrLogoOrArt(prompt: String, aspectRatio: String = "1:1"): Pair<Bitmap?, String?> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair(null, "Please configure your GEMINI_API_KEY in Settings / Secrets to generate AI logos and images.")
        }
        
        val enhancedPrompt = "Generate a high-contrast, professional, minimalist graphic logo or vector icon suitable for center placement on a QR code: $prompt. Clean edges, solid vibrant colors."
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = enhancedPrompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.1-flash-image-preview", apiKey, request)
            val parts = response.candidates?.firstOrNull()?.content?.parts ?: emptyList()
            var bitmap: Bitmap? = null
            var textMsg: String? = null

            for (part in parts) {
                if (part.inlineData != null) {
                    val base64Data = part.inlineData.data
                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } else if (part.text != null) {
                    textMsg = part.text
                }
            }
            Pair(bitmap, textMsg ?: "Image generated successfully with ratio $aspectRatio!")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Image generation error: ${e.message}")
        }
    }
}
