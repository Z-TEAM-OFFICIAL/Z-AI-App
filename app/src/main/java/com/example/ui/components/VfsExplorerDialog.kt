package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.artifacts.Artifact
import com.example.data.artifacts.ArtifactType
import com.example.data.vfs.VfsNode
import com.example.ui.theme.*
import com.example.ui.viewmodel.ZegaViewModel
import kotlinx.coroutines.launch

/**
 * Virtual File System (VFS) Visual Explorer.
 * Supports hierarchical tree navigation, file creation, editing, deletion,
 * sandboxed artifact preview, and cloud synchronization.
 */
@Composable
fun VfsExplorerDialog(
    viewModel: ZegaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf("/") }
    val vfsNodes by viewModel.vfsNodes.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()

    var selectedFile by remember { mutableStateOf<VfsNode?>(null) }
    var fileContentEditor by remember { mutableStateOf("") }
    var isEditingFile by remember { mutableStateOf(false) }

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newDirName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Filter current folder nodes
    val currentFolderItems = remember(vfsNodes, currentPath, searchQuery) {
        if (searchQuery.isNotBlank()) {
            vfsNodes.filter { it.name.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
        } else {
            val normalizedCurrent = if (currentPath == "/") "/" else currentPath.trimEnd('/')
            vfsNodes.filter { it.parentPath == normalizedCurrent }
                .sortedWith(compareByDescending<VfsNode> { it.isDirectory }.thenBy { it.name })
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp, max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(ZegaNeonCyan, ZegaNeonViolet)),
                    shape = RoundedCornerShape(20.dp)
                )
                .testTag("vfs_explorer_dialog"),
            colors = CardDefaults.cardColors(containerColor = ZegaDarkCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(ZegaNeonCyan, ZegaNeonViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Virtual File System", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Dual Persistence Engine (Room + Cloud)", fontSize = 10.sp, color = ZegaGrayText)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ZegaGrayText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search virtual files & code...", fontSize = 12.sp, color = ZegaGrayText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ZegaNeonCyan, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ZegaGrayText, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("vfs_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZegaNeonCyan,
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedContainerColor = Color(0xFF0F141C),
                        unfocusedContainerColor = Color(0xFF0F141C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Path Breadcrumbs & Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1117))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Breadcrumbs
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPath != "/") {
                                    val parent = currentPath.substringBeforeLast('/')
                                    currentPath = if (parent.isEmpty()) "/" else parent
                                    selectedFile = null
                                }
                            },
                            enabled = currentPath != "/",
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Up directory",
                                tint = if (currentPath != "/") ZegaNeonCyan else ZegaGrayText.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search results" else currentPath,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ZegaNeonCyan,
                            maxLines = 1
                        )
                    }

                    // New Folder & New File Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showCreateDirDialog = true },
                            modifier = Modifier.size(28.dp).testTag("vfs_new_folder_btn")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = ZegaNeonViolet, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { showCreateFileDialog = true },
                            modifier = Modifier.size(28.dp).testTag("vfs_new_file_btn")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = ZegaNeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main File List / Editor Area
                if (selectedFile != null) {
                    val file = selectedFile!!
                    // File Detail / Editor View
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = ZegaNeonCyan, modifier = Modifier.size(18.dp))
                                Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${file.sizeBytes} B", fontSize = 10.sp, color = ZegaGrayText)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isEditingFile) {
                                    IconButton(
                                        onClick = {
                                            viewModel.writeVfsFile(file.parentPath, file.name, fileContentEditor)
                                            isEditingFile = false
                                            Toast.makeText(context, "Saved changes to ${file.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = "Save", tint = ZegaPrivacyGreen, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            fileContentEditor = file.content
                                            isEditingFile = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ZegaNeonCyan, modifier = Modifier.size(18.dp))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteVfsNode(file.fullPath)
                                        selectedFile = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D6D), modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        selectedFile = null
                                        isEditingFile = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Back", tint = ZegaGrayText, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditingFile) {
                            OutlinedTextField(
                                value = fileContentEditor,
                                onValueChange = { fileContentEditor = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("vfs_file_editor_input"),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE6EDF3)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ZegaNeonCyan,
                                    unfocusedBorderColor = Color(0xFF30363D),
                                    focusedContainerColor = Color(0xFF080C12),
                                    unfocusedContainerColor = Color(0xFF080C12)
                                )
                            )
                        } else {
                            // Render code viewer or interactive sandboxed preview if HTML/JS
                            val artifactType = ArtifactType.fromString(file.fileExtension)
                            if (artifactType.isExecutable) {
                                SandboxedInteractivePreview(
                                    artifact = Artifact(
                                        title = file.name,
                                        type = artifactType,
                                        content = file.content
                                    )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF080C12))
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = file.content.ifEmpty { "(Empty file)" },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF58A6FF)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Tree List of Files and Directories
                    if (currentFolderItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching files found" else "This directory is empty",
                                fontSize = 12.sp,
                                color = ZegaGrayText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(currentFolderItems) { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF161B22))
                                        .clickable {
                                            if (node.isDirectory) {
                                                currentPath = node.fullPath
                                                searchQuery = ""
                                            } else {
                                                selectedFile = node
                                                fileContentEditor = node.content
                                                isEditingFile = false
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("vfs_node_${node.name}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (node.isDirectory) ZegaNeonViolet else ZegaNeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = node.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (node.isDirectory) "Directory" else "${node.sizeBytes} B • ${node.mimeType}",
                                                fontSize = 10.sp,
                                                color = ZegaGrayText
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteVfsNode(node.fullPath) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ZegaGrayText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${vfsNodes.size} total virtual nodes in VFS",
                        fontSize = 10.sp,
                        color = ZegaGrayText
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ZegaPrivacyGreen)
                        )
                        Text(
                            text = "Offline SQLite Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZegaPrivacyGreen
                        )
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create Virtual File", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("e.g. app.js, index.html, notes.txt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.writeVfsFile(currentPath, newFileName.trim(), "// Created in Z-AI VFS\n")
                            newFileName = ""
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZegaNeonCyan, contentColor = Color.Black)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") }
            },
            containerColor = ZegaDarkCard
        )
    }

    // New Folder Dialog
    if (showCreateDirDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = false },
            title = { Text("Create Directory", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    placeholder = { Text("e.g. components, scripts, docs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_dir_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDirName.isNotBlank()) {
                            viewModel.createVfsDirectory(currentPath, newDirName.trim())
                            newDirName = ""
                            showCreateDirDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZegaNeonViolet, contentColor = Color.White)
                ) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = false }) { Text("Cancel") }
            },
            containerColor = ZegaDarkCard
        )
    }
}
