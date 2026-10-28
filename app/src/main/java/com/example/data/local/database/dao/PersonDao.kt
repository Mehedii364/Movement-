package com.example.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.database.entities.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY personName ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE isActive = 1 ORDER BY personName ASC")
    fun getActivePersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    fun getPersonById(id: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonByIdDirect(id: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE personCode = :code LIMIT 1")
    suspend fun getPersonByCode(code: String): PersonEntity?

    @Query("SELECT COUNT(*) FROM persons")
    fun getTotalPersonsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM persons WHERE currentStatus = :status AND isActive = 1")
    fun getCountByStatus(status: String): Flow<Int>

    @Query("SELECT * FROM persons WHERE currentStatus = :status AND isActive = 1")
    fun getPersonsByStatus(status: String): Flow<List<PersonEntity>>

    @Query("""
        SELECT * FROM persons 
        WHERE (personName LIKE '%' || :query || '%' OR personCode LIKE '%' || :query || '%' OR department LIKE '%' || :query || '%')
        ORDER BY personName ASC
    """)
    fun searchPersons(query: String): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("UPDATE persons SET currentStatus = :status, updatedAt = :updatedAt WHERE id = :personId")
    suspend fun updatePersonStatus(personId: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePersonById(id: Long)
}
