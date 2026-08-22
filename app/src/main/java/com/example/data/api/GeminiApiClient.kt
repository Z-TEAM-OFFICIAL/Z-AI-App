package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "tools") val tools: List<GeminiTool>? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?,
    @Json(name = "groundingMetadata") val groundingMetadata: GeminiGroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// Veo specific requests
@JsonClass(generateAdapter = true)
data class VeoConfig(
    @Json(name = "numberOfVideos") val numberOfVideos: Int = 1,
    @Json(name = "aspectRatio") val aspectRatio: String = "16:9", // "16:9" or "9:16"
    @Json(name = "resolution") val resolution: String = "720p"
)

@JsonClass(generateAdapter = true)
data class VeoImage(
    @Json(name = "imageBytes") val imageBytes: String
)

@JsonClass(generateAdapter = true)
data class VeoGenerateRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "image") val image: VeoImage? = null,
    @Json(name = "config") val config: VeoConfig? = null
)

@JsonClass(generateAdapter = true)
data class VeoOperationResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "done") val done: Boolean? = false
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: VeoGenerateRequest
    ): VeoOperationResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        return if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") "" else apiKey
    }

    /**
     * Dynamic Multi-Tier Gemini model caller.
     * Supports:
     * - gemini-3.7-flash (Coding, math, and harder multi-step tasks)
     * - gemini-3.6-flash (Average assistant queries and explanations)
     * - gemini-3.5-flash-lite (Simple quick questions & fast difficulty classification)
     */
    suspend fun generateContent(
        prompt: String,
        systemInstructionText: String? = null,
        model: String = "gemini-3.6-flash"
    ): String? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val candidateModels = listOf(
            model,
            when (model) {
                "gemini-3.7-flash" -> "gemini-2.5-pro"
                "gemini-3.6-flash" -> "gemini-2.5-flash"
                "gemini-3.5-flash-lite" -> "gemini-2.5-flash-lite"
                else -> "gemini-2.5-flash"
            },
            "gemini-2.5-flash"
        ).distinct()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = systemInstructionText?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        for (targetModel in candidateModels) {
            try {
                val response = apiService.generateContent(targetModel, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: Exception) {
                // Continue to next model fallback
            }
        }
        return null
    }

    /**
     * Decides task difficulty level using gemini-3.5-flash-lite (or local heuristics).
     * Returns: "COMPLEX" (coding, harder work), "AVERAGE" (standard), or "SIMPLE" (basic Q&A).
     */
    suspend fun classifyTaskDifficulty(prompt: String): String {
        val lower = prompt.lowercase().trim()
        
        // Fast local checks for instant coding / file creation classification
        if (lower.contains("code") || lower.contains("html") || lower.contains("javascript") ||
            lower.contains("css") || lower.contains("python") || lower.contains("kotlin") ||
            lower.contains("make") && lower.contains("file") || lower.contains("create") && lower.contains("file") ||
            lower.contains("function") || lower.contains("algorithm") || lower.contains("program") ||
            lower.contains("regex") || lower.contains("sql") || lower.contains("index.html")) {
            return "COMPLEX"
        }

        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return if (prompt.length > 120 || prompt.contains("explain in detail") || prompt.contains("plan") || prompt.contains("calculate")) {
                "AVERAGE"
            } else {
                "SIMPLE"
            }
        }

        val classificationPrompt = """
            Classify the difficulty and nature of this user request into exactly ONE of these three uppercase words:
            - COMPLEX (if it involves coding, writing scripts/HTML/JS, complex math, multi-step problem solving, or difficult technical tasks)
            - AVERAGE (if it is a normal general inquiry, medium-length explanation, creative writing, or standard assistant task)
            - SIMPLE (if it is a short, simple factual question, greeting, or basic quick query)

            User Request: "$prompt"

            Respond with ONLY the single word (COMPLEX, AVERAGE, or SIMPLE):
        """.trimIndent()

        val result = generateContent(
            prompt = classificationPrompt,
            model = "gemini-3.5-flash-lite"
        )?.trim()?.uppercase() ?: "AVERAGE"

        return when {
            result.contains("COMPLEX") -> "COMPLEX"
            result.contains("SIMPLE") -> "SIMPLE"
            else -> "AVERAGE"
        }
    }

    /**
     * Google Search Grounding with gemini-3.5-flash
     * Uses Google Search tool to fetch live, up-to-date web information with sources and citations.
     */
    suspend fun generateWithSearchGrounding(
        prompt: String,
        systemInstructionText: String? = null
    ): SearchGroundingResult? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val model = "gemini-3.5-flash"
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = systemInstructionText?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            },
            tools = listOf(
                GeminiTool(googleSearch = GeminiGoogleSearch())
            )
        )

        return try {
            val response = apiService.generateContent(model, apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.firstOrNull()?.text ?: return null

            val grounding = candidate.groundingMetadata
            val queries = grounding?.webSearchQueries ?: emptyList()
            val chunks = grounding?.searchChunks ?: grounding?.groundingChunks ?: emptyList()

            val sources = chunks.mapNotNull { chunk ->
                val web = chunk.web
                if (web?.uri != null && web.title != null) {
                    GroundingSource(title = web.title, url = web.uri)
                } else null
            }.distinctBy { it.url }

            SearchGroundingResult(
                replyText = text,
                searchQueries = queries,
                sources = sources,
                isGrounded = queries.isNotEmpty() || sources.isNotEmpty()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Veo Video Generation using veo-3.1-fast-generate-preview
     * Supports image-to-video and text-to-video with aspect ratios 16:9 or 9:16.
     */
    suspend fun generateVeoVideo(
        prompt: String,
        imageBase64: String? = null,
        aspectRatio: String = "16:9"
    ): VeoOperationResponse? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val model = "veo-3.1-fast-generate-preview"
        val request = VeoGenerateRequest(
            prompt = prompt,
            image = imageBase64?.let { VeoImage(imageBytes = it) },
            config = VeoConfig(
                numberOfVideos = 1,
                aspectRatio = if (aspectRatio == "9:16") "9:16" else "16:9",
                resolution = "720p"
            )
        )

        return try {
            apiService.generateVideos(model, apiKey, request)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lyria Music Generation using lyria-3-clip-preview (short clip up to 30s) or lyria-3-pro-preview (full track)
     */
    suspend fun generateMusicTrack(
        prompt: String,
        isFullTrack: Boolean = false
    ): ByteArray? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val model = if (isFullTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview"
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Generate music audio: $prompt")))
            ),
            generationConfig = GeminiGenerationConfig(
                responseModalities = listOf("AUDIO")
            )
        )

        return try {
            val response = apiService.generateContent(model, apiKey, request)
            val inlineData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData
            if (inlineData?.data != null) {
                android.util.Base64.decode(inlineData.data, android.util.Base64.DEFAULT)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
