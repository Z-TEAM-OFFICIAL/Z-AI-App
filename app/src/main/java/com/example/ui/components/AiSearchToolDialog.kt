package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.GroundingSource
import com.example.data.api.ScrapedResult
import com.example.data.api.SearchGroundingResult
import com.example.ui.screens.MarkdownText
import com.example.ui.theme.*
import com.example.ui.viewmodel.ZegaViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiSearchToolDialog(
    viewModel: ZegaViewModel,
    onDismiss: () -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchInput by remember { mutableStateOf("") }
    val isSearching by viewModel.isAiSearching.collectAsState()
    val currentSearchQuery by viewModel.aiSearchQuery.collectAsState()
    val groundedResult by viewModel.lastGroundingResult.collectAsState()
    val scrapedResults by viewModel.lastScrapedResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isGroundedMode by viewModel.isSearchGroundingMode.collectAsState()

    val quickTopics = listOf(
        "Latest AI breakthroughs",
        "Kotlin Android innovations",
        "World technology headlines",
        "Space exploration news",
        "Global weather & climate"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(ZegaNeonCyan, ZegaPrivacyGreen, ZegaNeonViolet))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .testTag("ai_search_tool_dialog"),
            colors = CardDefaults.cardColors(containerColor = CosmicBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ZegaPrivacyGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = ZegaPrivacyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "AI WEB SEARCH TOOL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ZegaNeonCyan,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ZegaPrivacyGreen)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isGroundedMode) "GROUNDED" else "WEB SCRAPE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            Text(
                                text = "Real-time Google search grounding & multi-source web intelligence",
                                fontSize = 10.sp,
                                color = ZegaGrayText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_ai_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ZegaWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCard)
                        .border(1.dp, if (isSearching) ZegaNeonCyan else CosmicCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearching) ZegaNeonCyan else ZegaGrayText,
                        modifier = Modifier.size(20.dp)
                    )

                    TextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = {
                            Text(
                                text = "Ask anything or enter a search query...",
                                fontSize = 12.sp,
                                color = ZegaGrayText
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = ZegaWhite,
                            unfocusedTextColor = ZegaWhite,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchInput.isNotBlank()) {
                                    viewModel.executeAiSearch(searchInput.trim(), context)
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_search_text_input")
                    )

                    if (searchInput.isNotEmpty()) {
                        IconButton(
                            onClick = { searchInput = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = ZegaGrayText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (searchInput.isNotBlank()) {
                                viewModel.executeAiSearch(searchInput.trim(), context)
                            }
                        },
                        enabled = !isSearching && searchInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZegaPrivacyGreen,
                            disabledContainerColor = CosmicCardBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("execute_ai_search_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Engine Toggle & Mode Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = isGroundedMode,
                            onClick = { viewModel.setSearchGroundingMode(true) },
                            label = { Text("Google Search Grounding", fontSize = 10.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(12.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZegaPrivacyGreen.copy(alpha = 0.25f),
                                selectedLabelColor = ZegaPrivacyGreen,
                                selectedLeadingIconColor = ZegaPrivacyGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isGroundedMode,
                                borderColor = if (isGroundedMode) ZegaPrivacyGreen else CosmicCardBorder
                            )
                        )

                        FilterChip(
                            selected = !isGroundedMode,
                            onClick = { viewModel.setSearchGroundingMode(false) },
                            label = { Text("Direct Web Scraper", fontSize = 10.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(12.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZegaNeonCyan.copy(alpha = 0.25f),
                                selectedLabelColor = ZegaNeonCyan,
                                selectedLeadingIconColor = ZegaNeonCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !isGroundedMode,
                                borderColor = if (!isGroundedMode) ZegaNeonCyan else CosmicCardBorder
                            )
                        )
                    }

                    if (groundedResult != null || scrapedResults.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearAiSearchResults() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear Results", fontSize = 10.sp, color = ZegaWarningRed)
                        }
                    }
                }

                // Quick Topic Suggestions
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickTopics) { topic ->
                        SuggestionChip(
                            onClick = {
                                searchInput = topic
                                viewModel.executeAiSearch(topic, context)
                            },
                            label = {
                                Text(
                                    text = topic,
                                    fontSize = 10.sp,
                                    color = ZegaLightGrayText
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = CosmicCard
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = CosmicCardBorder
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Divider(
                    color = CosmicCardBorder.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Results Content View
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = ZegaPrivacyGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Searching live web & synthesizing AI answer...",
                                fontSize = 12.sp,
                                color = ZegaLightGrayText,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Query: \"${currentSearchQuery}\"",
                                fontSize = 10.sp,
                                color = ZegaNeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else if (groundedResult != null || scrapedResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("ai_search_results_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Synthesized AI Answer
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                                border = BorderStroke(1.dp, ZegaPrivacyGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = ZegaPrivacyGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "SYNTHESIZED AI ANSWER",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ZegaPrivacyGreen,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val reply = groundedResult?.replyText ?: scrapedResults.firstOrNull()?.snippet ?: ""
                                                    viewModel.speakLocal(reply)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Speak Answer",
                                                    tint = ZegaNeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val reply = groundedResult?.replyText ?: scrapedResults.firstOrNull()?.snippet ?: ""
                                                    clipboardManager.setText(AnnotatedString(reply))
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Answer",
                                                    tint = ZegaWhite,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val answerText = groundedResult?.replyText ?: scrapedResults.firstOrNull()?.snippet ?: "No answer found."
                                    MarkdownText(
                                        text = answerText,
                                        textColor = ZegaWhite
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.addAiSearchAnswerToChat(currentSearchQuery)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZegaDeepIndigo),
                                        border = BorderStroke(1.dp, ZegaNeonCyan.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = ZegaNeonCyan, modifier = Modifier.size(12.dp))
                                            Text("Send to Main Chat", fontSize = 10.sp, color = ZegaWhite, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Grounded Web Sources
                        val sources = groundedResult?.sources ?: emptyList()
                        if (sources.isNotEmpty()) {
                            item {
                                Text(
                                    text = "LIVE WEB SOURCES & CITATIONS (${sources.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaNeonCyan,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(sources) { source ->
                                WebSourceCard(
                                    title = source.title,
                                    url = source.url,
                                    domain = source.domain,
                                    onOpen = { onOpenInBrowser(source.url) }
                                )
                            }
                        }

                        // 3. Scraped Results
                        if (scrapedResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "INDEXED SEARCH RESULTS (${scrapedResults.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaNeonCyan,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(scrapedResults) { result ->
                                ScrapedResultCard(
                                    result = result,
                                    onOpen = { onOpenInBrowser(result.link) }
                                )
                            }
                        }

                        // 4. Related Queries
                        val related = groundedResult?.searchQueries ?: emptyList()
                        if (related.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(top = 6.dp)) {
                                    Text(
                                        text = "RELATED SEARCH QUERIES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZegaGrayText,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        related.forEach { query ->
                                            SuggestionChip(
                                                onClick = {
                                                    searchInput = query
                                                    viewModel.executeAiSearch(query, context)
                                                },
                                                label = { Text(query, fontSize = 9.sp, color = ZegaNeonCyan) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CosmicCard),
                                                border = SuggestionChipDefaults.suggestionChipBorder(
                                                    enabled = true,
                                                    borderColor = CosmicCardBorder
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty Search Landing State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ZegaDeepIndigo.copy(alpha = 0.5f))
                                    .border(1.dp, ZegaNeonCyan.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ZegaNeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Search the Web with AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZegaWhite
                            )
                            Text(
                                text = "Z-AI fetches real-time web intelligence, live Google grounding citations, and summarizes topics directly.",
                                fontSize = 11.sp,
                                color = ZegaGrayText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )

                            if (searchHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "RECENT SEARCHES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZegaNeonCyan
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    searchHistory.take(4).forEach { prevQuery ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CosmicCard)
                                                .clickable {
                                                    searchInput = prevQuery
                                                    viewModel.executeAiSearch(prevQuery, context)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = ZegaGrayText, modifier = Modifier.size(14.dp))
                                                Text(prevQuery, fontSize = 11.sp, color = ZegaLightGrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ZegaNeonCyan, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebSourceCard(
    title: String,
    url: String,
    domain: String,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        border = BorderStroke(1.dp, CosmicCardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ZegaPrivacyGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = ZegaPrivacyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZegaWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = domain,
                        fontSize = 10.sp,
                        color = ZegaPrivacyGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(
                onClick = onOpen,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Open in Browser",
                    tint = ZegaNeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ScrapedResultCard(
    result: ScrapedResult,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        border = BorderStroke(1.dp, CosmicCardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = result.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZegaNeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open in Browser",
                        tint = ZegaNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.snippet,
                fontSize = 11.sp,
                color = ZegaLightGrayText,
                lineHeight = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.link,
                fontSize = 9.sp,
                color = ZegaGrayText,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
