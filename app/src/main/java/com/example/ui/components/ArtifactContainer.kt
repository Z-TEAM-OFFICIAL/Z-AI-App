package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.artifacts.Artifact
import com.example.data.artifacts.ArtifactType
import com.example.ui.theme.*

/**
 * Visual Artifact Container.
 * Holds code/markup payloads, completely bypassed by TTS reader loop.
 * Features tabs for Source Code vs Live Interactive Sandboxed Execution,
 * with direct export to the Virtual File System.
 */
@Composable
fun ArtifactContainer(
    artifact: Artifact,
    onSaveToVfs: ((Artifact) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(if (artifact.type.isExecutable) 1 else 0) } // Default to Live Preview if executable
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ZegaNeonCyan.copy(alpha = 0.5f),
                        ZegaNeonViolet.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("artifact_container_${artifact.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F141C)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (artifact.type) {
                                    ArtifactType.HTML -> ZegaNeonCyan.copy(alpha = 0.2f)
                                    ArtifactType.JAVASCRIPT -> Color(0xFFFFD166).copy(alpha = 0.2f)
                                    ArtifactType.JSON -> ZegaNeonViolet.copy(alpha = 0.2f)
                                    ArtifactType.MARKDOWN -> ZegaPrivacyGreen.copy(alpha = 0.2f)
                                    else -> ZegaPulseOrange.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = artifact.type.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (artifact.type) {
                                ArtifactType.HTML -> ZegaNeonCyan
                                ArtifactType.JAVASCRIPT -> Color(0xFFFFD166)
                                ArtifactType.JSON -> ZegaNeonViolet
                                ArtifactType.MARKDOWN -> ZegaPrivacyGreen
                                else -> ZegaPulseOrange
                            }
                        )
                    }

                    Text(
                        text = artifact.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Copy Code Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Artifact Content", artifact.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Artifact code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_artifact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Artifact",
                            tint = ZegaGrayText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Save to Virtual File System
                    if (onSaveToVfs != null) {
                        IconButton(
                            onClick = { onSaveToVfs(artifact) },
                            modifier = Modifier.size(32.dp).testTag("save_artifact_vfs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save to VFS",
                                tint = ZegaNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Expand / Collapse Toggle
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp).testTag("expand_artifact_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Expand",
                            tint = ZegaGrayText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Tab Selector Bar
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF161B22),
                        contentColor = ZegaNeonCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 2.dp,
                                color = ZegaNeonCyan
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .height(36.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("SOURCE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )

                        if (artifact.type.isExecutable) {
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("LIVE PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab Content
                    if (selectedTab == 0) {
                        // Source Code Viewer with Syntax Style & Line Numbers
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 260.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF080C12))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = artifact.content,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF58A6FF),
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        // Live Sandboxed Interactive Execution
                        SandboxedInteractivePreview(artifact = artifact)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Footer Meta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Path: ${artifact.virtualPath}",
                            fontSize = 9.sp,
                            color = ZegaGrayText
                        )
                        Text(
                            text = "TTS Audio: Excluded (Metadata Only)",
                            fontSize = 9.sp,
                            color = ZegaPrivacyGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
