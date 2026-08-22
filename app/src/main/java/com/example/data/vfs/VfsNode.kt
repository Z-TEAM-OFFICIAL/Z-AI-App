package com.example.data.vfs

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "vfs_nodes",
    indices = [
        Index(value = ["fullPath"], unique = true),
        Index(value = ["parentPath"])
    ]
)
data class VfsNode(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentPath: String, // e.g. "/" or "/projects"
    val fullPath: String,   // e.g. "/projects/index.html"
    val isDirectory: Boolean = false,
    val content: String = "",
    val mimeType: String = "text/plain",
    val sizeBytes: Long = content.toByteArray().size.toLong(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCloudSynced: Boolean = false
) {
    val fileExtension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")
}
