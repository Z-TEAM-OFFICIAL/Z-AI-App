package com.example.data.api

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

data class ScrapedResult(
    val title: String,
    val link: String,
    val snippet: String
)

object GoogleSearchScraper {
    private const val TAG = "GoogleSearchScraper"

    fun search(query: String): List<ScrapedResult> {
        val results = mutableListOf<ScrapedResult>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            // Query 5 results
            val urlString = "https://www.google.com/search?q=$encodedQuery&num=6"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            // Desktop browser user-agent yields stable and predictable HTML structure
            connection.setRequestProperty(
                "User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
            )
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Search request returned non-OK status: $responseCode")
                return results
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val html = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                html.append(line).append("\n")
            }
            reader.close()

            val htmlContent = html.toString()

            // In Google's desktop results, search result groups are contained inside divs like class="g" or class="MjjY6e".
            // Let's split on `<div class="g"` or `<div class="MjjY6e"` or `<div class="tF2Cxc"` to parse each search card.
            var blocks = htmlContent.split("<div class=\"g\"")
            if (blocks.size <= 1) {
                blocks = htmlContent.split("<div class=\"MjjY6e\"")
            }
            if (blocks.size <= 1) {
                blocks = htmlContent.split("<div class=\"tF2Cxc\"")
            }

            val h3Regex = """<h3[^>]*>(.*?)</h3>""".toRegex()
            val linkRegex = """<a href="([^"]+)"[^>]*>""".toRegex()

            for (i in 1 until blocks.size) {
                val block = blocks[i]
                
                // Extract the first h3 in this result block, which is the page title
                val h3Match = h3Regex.find(block)
                if (h3Match != null) {
                    val rawTitle = h3Match.groupValues[1]
                    val title = stripHtml(rawTitle)

                    // Find first hyperlink
                    val linkMatch = linkRegex.find(block)
                    var link = linkMatch?.groupValues?.get(1) ?: ""
                    
                    // Decode Google redirection URL if needed
                    if (link.startsWith("/url?q=")) {
                        link = link.substringAfter("/url?q=").substringBefore("&")
                        link = URLDecoder.decode(link, "UTF-8")
                    }

                    if (link.isNotEmpty() && !link.contains("google.com") && link.startsWith("http")) {
                        // Extract snippet description text
                        val snippet = extractSnippet(block, rawTitle)
                        results.add(ScrapedResult(title, link, snippet))
                    }
                }
                if (results.size >= 5) break
            }

            // Broad-regex fallback if no blocks matched
            if (results.isEmpty()) {
                val matches = h3Regex.findAll(htmlContent).toList()
                for (match in matches) {
                    val rawTitle = match.groupValues[1]
                    val title = stripHtml(rawTitle)

                    // Look inside nearby region in the HTML
                    val matchPos = match.range.first
                    val startIdx = maxOf(0, matchPos - 300)
                    val endIdx = minOf(htmlContent.length, matchPos + 1200)
                    val neighborhood = htmlContent.substring(startIdx, endIdx)

                    val linkMatch = linkRegex.find(neighborhood)
                    var link = linkMatch?.groupValues?.get(1) ?: ""
                    if (link.startsWith("/url?q=")) {
                        link = link.substringAfter("/url?q=").substringBefore("&")
                        link = URLDecoder.decode(link, "UTF-8")
                    }

                    if (link.isNotEmpty() && !link.contains("google.com") && link.startsWith("http")) {
                        val snippet = "Web content retrieved directly from Google Search."
                        results.add(ScrapedResult(title, link, snippet))
                        if (results.size >= 4) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during direct search scraping: ${e.message}", e)
        }
        return results
    }

    private fun stripHtml(html: String): String {
        return html.replace("<[^>]*>".toRegex(), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun extractSnippet(block: String, rawTitle: String): String {
        // Remove the title portion to isolate description text
        val remainder = block.replace(rawTitle, "")
        
        // Find segments between elements that aren't markup
        val snippetCandidates = """<div[^>]*class="[^"]*(?:VwiC3b|yDAB2d|s3rec|b77sfe)[^"]*"[^>]*>(.*?)</div>|<span[^>]*class="[^"]*(?:VwiC3b|yDAB2d|s3rec|b77sfe)[^"]*"[^>]*>(.*?)</span>""".toRegex()
            .findAll(remainder)
            .map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
            .map { stripHtml(it) }
            .filter { it.length > 15 && !it.contains("<") }
            .toList()

        if (snippetCandidates.isNotEmpty()) {
            return snippetCandidates.first()
        }

        // Broad fallback: strip tags from remainder and slice a nice chunk
        val plainText = stripHtml(remainder)
        return if (plainText.length > 160) {
            plainText.take(160) + "..."
        } else if (plainText.isNotEmpty()) {
            plainText
        } else {
            "Open the web link to view contents."
        }
    }
}
