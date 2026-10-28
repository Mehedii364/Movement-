package com.example.data.local.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class MovementWithPerson(
    @Embedded
    val movement: MovementEntity,
    @Relation(
        parentColumn = "personId",
        entityColumn = "id"
    )
    val person: PersonEntity
)
