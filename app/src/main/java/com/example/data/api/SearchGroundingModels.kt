package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroundingSource(
    val title: String,
    val url: String,
    val domain: String = url.substringAfter("://").substringBefore("/")
)

@JsonClass(generateAdapter = true)
data class SearchGroundingResult(
    val replyText: String,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val isGrounded: Boolean = false
)

// Moshi data models for Gemini Google Search Tool & Grounding Metadata
@JsonClass(generateAdapter = true)
data class GeminiGoogleSearch(
    @Json(name = "threshold") val threshold: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    @Json(name = "googleSearch") val googleSearch: GeminiGoogleSearch? = null
)

@JsonClass(generateAdapter = true)
data class GeminiWebSource(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSearchChunk(
    @Json(name = "web") val web: GeminiWebSource? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null,
    @Json(name = "searchChunks") val searchChunks: List<GeminiSearchChunk>? = null,
    @Json(name = "groundingChunks") val groundingChunks: List<GeminiSearchChunk>? = null
)
