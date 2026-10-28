package com.example.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // "PERSON" or "MOVEMENT"
    val recordId: Long,
    val uuid: String,
    val action: String, // "CREATE" or "UPDATE"
    val status: String = "PENDING", // "PENDING", "IN_PROGRESS", "FAILED", "SYNCED"
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
