package com.example.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["personId"]),
        Index(value = ["movementTimestamp"]),
        Index(value = ["movementType"]),
        Index(value = ["syncStatus"])
    ]
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String,
    val personId: Long,
    val movementType: String, // "IN" or "OUT"
    val purpose: String,
    val destination: String,
    val remarks: String = "",
    val movementTimestamp: Long, // Epoch millis
    val durationMinutes: Long? = null, // Completed outside duration in minutes for IN
    val recordedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING" // "PENDING", "SYNCED", "FAILED"
)
