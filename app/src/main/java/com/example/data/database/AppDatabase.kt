package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.data.vfs.VfsDao
import com.example.data.vfs.VfsNode

@Database(entities = [ChatMessage::class, VoiceNote::class, UserAccount::class, AICreation::class, VfsNode::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zegaDao(): ZegaDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun vfsDao(): VfsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zega_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
