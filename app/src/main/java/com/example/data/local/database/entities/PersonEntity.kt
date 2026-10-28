package com.example.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persons",
    indices = [
        Index(value = ["personCode"], unique = true),
        Index(value = ["personName"]),
        Index(value = ["department"])
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val personCode: String,
    val photoPath: String? = null,
    val designation: String,
    val department: String,
    val mobile: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val currentStatus: String = "INSIDE", // "INSIDE" or "OUTSIDE"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
