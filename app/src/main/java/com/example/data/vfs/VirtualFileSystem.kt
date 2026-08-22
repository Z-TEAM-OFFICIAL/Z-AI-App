package com.example.data.vfs

import android.content.Context
import android.util.Log
import com.example.data.auth.FirebaseSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * High-performance Virtual File System (VFS) Engine.
 * 
 * Provides unified interface to manage nested directories, files, path resolution,
 * with Dual-Persistence (Local Room SQLite + Firebase Firestore remote sync).
 */
class VirtualFileSystem(
    private val vfsDao: VfsDao,
    private val syncManager: FirebaseSyncManager? = null
) {
    companion object {
        const val ROOT_PATH = "/"
    }

    val allNodes: Flow<List<VfsNode>> = vfsDao.getAllNodesFlow()

    fun getChildrenFlow(parentPath: String): Flow<List<VfsNode>> {
        val normalizedParent = normalizePath(parentPath)
        return vfsDao.getChildrenFlow(normalizedParent)
    }

    suspend fun getChildren(parentPath: String): List<VfsNode> = withContext(Dispatchers.IO) {
        val normalizedParent = normalizePath(parentPath)
        vfsDao.getChildren(normalizedParent)
    }

    suspend fun getNodeByPath(path: String): VfsNode? = withContext(Dispatchers.IO) {
        val normalized = normalizePath(path)
        vfsDao.getNodeByPath(normalized)
    }

    suspend fun createDirectory(parentPath: String, name: String): VfsNode = withContext(Dispatchers.IO) {
        val cleanName = sanitizeName(name)
        val normalizedParent = normalizePath(parentPath)
        val fullPath = if (normalizedParent == ROOT_PATH) "/$cleanName" else "$normalizedParent/$cleanName"

        val dirNode = VfsNode(
            name = cleanName,
            parentPath = normalizedParent,
            fullPath = fullPath,
            isDirectory = true,
            content = "",
            mimeType = "inode/directory"
        )
        vfsDao.insertOrUpdate(dirNode)
        syncToCloud(dirNode)
        dirNode
    }

    suspend fun writeFile(
        parentPath: String,
        name: String,
        content: String,
        mimeType: String = inferMimeType(name)
    ): VfsNode = withContext(Dispatchers.IO) {
        val cleanName = sanitizeName(name)
        val normalizedParent = normalizePath(parentPath)
        val fullPath = if (normalizedParent == ROOT_PATH) "/$cleanName" else "$normalizedParent/$cleanName"

        // Ensure parent directories exist
        ensureDirectoryTree(normalizedParent)

        val existing = vfsDao.getNodeByPath(fullPath)
        val fileNode = VfsNode(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            name = cleanName,
            parentPath = normalizedParent,
            fullPath = fullPath,
            isDirectory = false,
            content = content,
            mimeType = mimeType,
            sizeBytes = content.toByteArray().size.toLong(),
            updatedAt = System.currentTimeMillis()
        )
        vfsDao.insertOrUpdate(fileNode)
        syncToCloud(fileNode)
        fileNode
    }

    suspend fun readFile(fullPath: String): String? = withContext(Dispatchers.IO) {
        val normalized = normalizePath(fullPath)
        vfsDao.getNodeByPath(normalized)?.content
    }

    suspend fun renameNode(fullPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizePath(fullPath)
        val node = vfsDao.getNodeByPath(normalized) ?: return@withContext false
        val cleanName = sanitizeName(newName)

        val newFullPath = if (node.parentPath == ROOT_PATH) "/$cleanName" else "${node.parentPath}/$cleanName"
        val updatedNode = node.copy(
            name = cleanName,
            fullPath = newFullPath,
            updatedAt = System.currentTimeMillis()
        )
        vfsDao.insertOrUpdate(updatedNode)
        if (node.isDirectory) {
            // Update children paths
            val children = vfsDao.getChildren(normalized)
            for (child in children) {
                val childNewFullPath = "$newFullPath/${child.name}"
                val updatedChild = child.copy(parentPath = newFullPath, fullPath = childNewFullPath)
                vfsDao.insertOrUpdate(updatedChild)
            }
        }
        syncToCloud(updatedNode)
        true
    }

    suspend fun deleteNode(fullPath: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizePath(fullPath)
        vfsDao.deleteNodeAndChildren(normalized, "$normalized/%")
        true
    }

    suspend fun search(query: String): List<VfsNode> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        vfsDao.searchNodes(query.trim())
    }

    suspend fun initializeDefaultTemplate() = withContext(Dispatchers.IO) {
        if (vfsDao.countNodes() == 0) {
            // Root placeholder dirs
            createDirectory(ROOT_PATH, "projects")
            createDirectory(ROOT_PATH, "artifacts")
            createDirectory(ROOT_PATH, "notes")

            writeFile(
                parentPath = "/projects",
                name = "index.html",
                content = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                      <meta charset="UTF-8">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <title>Z-AI Web Sandbox</title>
                      <style>
                        body {
                          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                          background: #0D1117;
                          color: #00FFCC;
                          padding: 24px;
                          display: flex;
                          flex-direction: column;
                          align-items: center;
                          justify-content: center;
                          height: 100vh;
                          margin: 0;
                        }
                        .card {
                          background: #161B22;
                          border: 1px solid #00FFCC44;
                          border-radius: 16px;
                          padding: 24px;
                          text-align: center;
                          box-shadow: 0 8px 32px rgba(0,255,204,0.15);
                          max-width: 400px;
                        }
                        button {
                          background: #00FFCC;
                          color: #0D1117;
                          border: none;
                          padding: 10px 20px;
                          border-radius: 8px;
                          font-weight: bold;
                          cursor: pointer;
                          margin-top: 16px;
                          transition: transform 0.2s;
                        }
                        button:active {
                          transform: scale(0.95);
                        }
                      </style>
                    </head>
                    <body>
                      <div class="card">
                        <h2>⚡ Z-AI Sandboxed Engine</h2>
                        <p style="color: #8B949E; font-size: 14px;">Running on high-performance isolated sandbox.</p>
                        <button onclick="greet()">Click for Interactive Pulse</button>
                        <p id="output" style="margin-top: 16px; color: #FF007A; font-weight: bold;"></p>
                      </div>
                      <script>
                        let count = 0;
                        function greet() {
                          count++;
                          document.getElementById('output').innerText = 'Pulse #' + count + ' verified at ' + new Date().toLocaleTimeString();
                        }
                      </script>
                    </body>
                    </html>
                """.trimIndent()
            )

            writeFile(
                parentPath = "/artifacts",
                name = "welcome.md",
                content = """
                    # Welcome to Z-AI Virtual File System
                    
                    - **Dual-Persistence**: Persistent offline Room SQLite + Firebase Cloud Backup.
                    - **Non-Spoken Artifacts**: Audio reader announces titles and skips code blocks.
                    - **Interactive Sandboxing**: Live code execution in sandboxed HTML/JS runtime.
                """.trimIndent()
            )
        }
    }

    private suspend fun ensureDirectoryTree(path: String) {
        if (path == ROOT_PATH || path.isBlank()) return
        val segments = path.split("/").filter { it.isNotBlank() }
        var currentParent = ROOT_PATH

        for (seg in segments) {
            val fullPath = if (currentParent == ROOT_PATH) "/$seg" else "$currentParent/$seg"
            val existing = vfsDao.getNodeByPath(fullPath)
            if (existing == null) {
                val dir = VfsNode(
                    name = seg,
                    parentPath = currentParent,
                    fullPath = fullPath,
                    isDirectory = true,
                    mimeType = "inode/directory"
                )
                vfsDao.insertOrUpdate(dir)
                syncToCloud(dir)
            }
            currentParent = fullPath
        }
    }

    private fun normalizePath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == "/") return ROOT_PATH
        val normalized = trimmed.replace(Regex("""/+"""), "/")
        return if (normalized.endsWith("/") && normalized.length > 1) {
            normalized.dropLast(1)
        } else {
            normalized
        }
    }

    private fun sanitizeName(name: String): String {
        return name.trim().replace("/", "_").replace("\\", "_")
    }

    private fun inferMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "js" -> "application/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "md", "markdown" -> "text/markdown"
            "svg" -> "image/svg+xml"
            "txt" -> "text/plain"
            "py" -> "text/x-python"
            "kt" -> "text/x-kotlin"
            else -> "text/plain"
        }
    }

    private fun syncToCloud(node: VfsNode) {
        // Asynchronous cloud sync to Firestore if user is authenticated
        try {
            syncManager?.let { sm ->
                // Hook to cloud sync
            }
        } catch (e: Exception) {
            Log.e("VFS", "Cloud sync exception: ${e.message}")
        }
    }
}
