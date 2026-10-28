package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.database.entities.MovementEntity
import com.example.data.local.database.entities.MovementWithPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {
    @Transaction
    @Query("SELECT * FROM movements ORDER BY movementTimestamp DESC")
    fun getAllMovementsWithPerson(): Flow<List<MovementWithPerson>>

    @Transaction
    @Query("SELECT * FROM movements ORDER BY movementTimestamp DESC LIMIT :limit")
    fun getRecentMovementsWithPerson(limit: Int = 10): Flow<List<MovementWithPerson>>

    @Transaction
    @Query("SELECT * FROM movements WHERE personId = :personId ORDER BY movementTimestamp DESC")
    fun getMovementsForPerson(personId: Long): Flow<List<MovementWithPerson>>

    @Transaction
    @Query("SELECT * FROM movements WHERE personId = :personId ORDER BY movementTimestamp DESC")
    suspend fun getMovementsForPersonDirect(personId: Long): List<MovementWithPerson>

    @Transaction
    @Query("""
        SELECT * FROM movements 
        WHERE movementTimestamp >= :startTime AND movementTimestamp <= :endTime
        ORDER BY movementTimestamp DESC
    """)
    fun getMovementsBetween(startTime: Long, endTime: Long): Flow<List<MovementWithPerson>>

    @Transaction
    @Query("""
        SELECT * FROM movements 
        WHERE movementTimestamp >= :startTime AND movementTimestamp <= :endTime
        ORDER BY movementTimestamp DESC
    """)
    suspend fun getMovementsBetweenDirect(startTime: Long, endTime: Long): List<MovementWithPerson>

    @Query("SELECT * FROM movements WHERE personId = :personId ORDER BY movementTimestamp DESC LIMIT 1")
    suspend fun getLatestMovementForPerson(personId: Long): MovementEntity?

    @Query("SELECT * FROM movements WHERE id = :id LIMIT 1")
    suspend fun getMovementById(id: Long): MovementEntity?

    @Query("""
        SELECT COUNT(*) FROM movements 
        WHERE movementType = :type AND movementTimestamp >= :startTime AND movementTimestamp <= :endTime
    """)
    fun countMovementsByTypeAndRange(type: String, startTime: Long, endTime: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0) FROM movements 
        WHERE movementTimestamp >= :startTime AND movementTimestamp <= :endTime
    """)
    fun sumDurationMinutesInRange(startTime: Long, endTime: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: MovementEntity): Long

    @Update
    suspend fun updateMovement(movement: MovementEntity)

    @Delete
    suspend fun deleteMovement(movement: MovementEntity)

    @Query("SELECT * FROM movements WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSyncMovements(): List<MovementEntity>

    @Query("UPDATE movements SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())
}
