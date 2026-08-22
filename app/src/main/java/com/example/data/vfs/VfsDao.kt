package com.example.data.vfs

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VfsDao {
    @Query("SELECT * FROM vfs_nodes ORDER BY isDirectory DESC, name ASC")
    fun getAllNodesFlow(): Flow<List<VfsNode>>

    @Query("SELECT * FROM vfs_nodes WHERE parentPath = :parentPath ORDER BY isDirectory DESC, name ASC")
    fun getChildrenFlow(parentPath: String): Flow<List<VfsNode>>

    @Query("SELECT * FROM vfs_nodes WHERE parentPath = :parentPath ORDER BY isDirectory DESC, name ASC")
    suspend fun getChildren(parentPath: String): List<VfsNode>

    @Query("SELECT * FROM vfs_nodes WHERE fullPath = :fullPath LIMIT 1")
    suspend fun getNodeByPath(fullPath: String): VfsNode?

    @Query("SELECT * FROM vfs_nodes WHERE id = :id LIMIT 1")
    suspend fun getNodeById(id: String): VfsNode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(node: VfsNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<VfsNode>)

    @Query("DELETE FROM vfs_nodes WHERE fullPath = :fullPath OR fullPath LIKE :prefix")
    suspend fun deleteNodeAndChildren(fullPath: String, prefix: String)

    @Query("DELETE FROM vfs_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM vfs_nodes")
    suspend fun countNodes(): Int

    @Query("SELECT * FROM vfs_nodes WHERE name LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchNodes(query: String): List<VfsNode>

    @Query("DELETE FROM vfs_nodes")
    suspend fun clearAll()
}
