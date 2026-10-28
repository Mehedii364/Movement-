package com.example.data.repository

import com.example.data.local.database.MovementDatabase
import com.example.data.local.database.entities.MovementEntity
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.database.entities.PersonEntity
import com.example.data.local.database.entities.SyncQueueEntity
import com.example.domain.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class MovementRepository(
    private val database: MovementDatabase
) {
    private val personDao = database.personDao()
    private val movementDao = database.movementDao()
    private val syncQueueDao = database.syncQueueDao()

    val allPersons: Flow<List<PersonEntity>> = personDao.getAllPersons()
    val activePersons: Flow<List<PersonEntity>> = personDao.getActivePersons()
    val allMovementsWithPerson: Flow<List<MovementWithPerson>> = movementDao.getAllMovementsWithPerson()

    fun getRecentMovements(limit: Int = 10): Flow<List<MovementWithPerson>> =
        movementDao.getRecentMovementsWithPerson(limit)

    fun getPersonById(id: Long): Flow<PersonEntity?> = personDao.getPersonById(id)

    suspend fun getPersonByIdDirect(id: Long): PersonEntity? = personDao.getPersonByIdDirect(id)

    fun getMovementsForPerson(personId: Long): Flow<List<MovementWithPerson>> =
        movementDao.getMovementsForPerson(personId)

    suspend fun getMovementsForPersonDirect(personId: Long): List<MovementWithPerson> =
        movementDao.getMovementsForPersonDirect(personId)

    fun searchPersons(query: String): Flow<List<PersonEntity>> = personDao.searchPersons(query)

    suspend fun registerPerson(person: PersonEntity): Result<Long> {
        val existing = personDao.getPersonByCode(person.personCode.trim())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Person ID / Employee ID '${person.personCode}' already exists!"))
        }
        val id = personDao.insertPerson(person.copy(personCode = person.personCode.trim()))
        // Enqueue sync
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "PERSON",
                recordId = id,
                uuid = UUID.randomUUID().toString(),
                action = "CREATE"
            )
        )
        return Result.success(id)
    }

    suspend fun updatePerson(person: PersonEntity): Result<Unit> {
        val existing = personDao.getPersonByCode(person.personCode.trim())
        if (existing != null && existing.id != person.id) {
            return Result.failure(IllegalArgumentException("Another person with ID '${person.personCode}' already exists!"))
        }
        personDao.updatePerson(person.copy(updatedAt = System.currentTimeMillis()))
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "PERSON",
                recordId = person.id,
                uuid = UUID.randomUUID().toString(),
                action = "UPDATE"
            )
        )
        return Result.success(Unit)
    }

    suspend fun recordOutMovement(
        personId: Long,
        purpose: String,
        destination: String,
        remarks: String,
        recordedBy: String
    ): Result<Long> {
        val person = personDao.getPersonByIdDirect(personId)
            ?: return Result.failure(IllegalArgumentException("Selected person does not exist."))

        if (person.currentStatus == "OUTSIDE") {
            return Result.failure(
                IllegalStateException("Cannot record OUT entry: ${person.personName} (${person.personCode}) is already recorded as OUTSIDE.")
            )
        }

        val now = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString()
        val movement = MovementEntity(
            uuid = uuid,
            personId = personId,
            movementType = "OUT",
            purpose = purpose,
            destination = destination,
            remarks = remarks,
            movementTimestamp = now,
            durationMinutes = null,
            recordedBy = recordedBy,
            createdAt = now,
            updatedAt = now,
            syncStatus = "PENDING"
        )

        val id = movementDao.insertMovement(movement)
        personDao.updatePersonStatus(personId = personId, status = "OUTSIDE", updatedAt = now)

        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "MOVEMENT",
                recordId = id,
                uuid = uuid,
                action = "CREATE"
            )
        )

        return Result.success(id)
    }

    suspend fun recordInMovement(
        personId: Long,
        purpose: String,
        destination: String,
        remarks: String,
        recordedBy: String
    ): Result<Long> {
        val person = personDao.getPersonByIdDirect(personId)
            ?: return Result.failure(IllegalArgumentException("Selected person does not exist."))

        if (person.currentStatus == "INSIDE") {
            return Result.failure(
                IllegalStateException("Cannot record IN entry: ${person.personName} (${person.personCode}) is already recorded as INSIDE.")
            )
        }

        val now = System.currentTimeMillis()
        val latestMovement = movementDao.getLatestMovementForPerson(personId)
        val calculatedDuration: Long? = if (latestMovement != null && latestMovement.movementType == "OUT") {
            TimeUtils.calculateDurationMinutes(latestMovement.movementTimestamp, now)
        } else {
            null
        }

        val uuid = UUID.randomUUID().toString()
        val movement = MovementEntity(
            uuid = uuid,
            personId = personId,
            movementType = "IN",
            purpose = purpose,
            destination = destination,
            remarks = remarks,
            movementTimestamp = now,
            durationMinutes = calculatedDuration,
            recordedBy = recordedBy,
            createdAt = now,
            updatedAt = now,
            syncStatus = "PENDING"
        )

        val id = movementDao.insertMovement(movement)
        personDao.updatePersonStatus(personId = personId, status = "INSIDE", updatedAt = now)

        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "MOVEMENT",
                recordId = id,
                uuid = uuid,
                action = "CREATE"
            )
        )

        return Result.success(id)
    }

    suspend fun correctMovementRecord(
        movementId: Long,
        newPurpose: String,
        newDestination: String,
        newRemarks: String,
        operatorName: String,
        correctionReason: String
    ): Result<Unit> {
        val movement = movementDao.getMovementById(movementId)
            ?: return Result.failure(IllegalArgumentException("Movement record not found."))

        val auditNote = "[Corrected by $operatorName: $correctionReason]"
        val updatedRemarks = if (movement.remarks.isBlank()) auditNote else "${movement.remarks} | $auditNote"

        val updated = movement.copy(
            purpose = newPurpose,
            destination = newDestination,
            remarks = updatedRemarks,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )

        movementDao.updateMovement(updated)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "MOVEMENT",
                recordId = updated.id,
                uuid = updated.uuid,
                action = "UPDATE"
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteMovementRecord(movementId: Long): Result<Unit> {
        val movement = movementDao.getMovementById(movementId)
            ?: return Result.failure(IllegalArgumentException("Movement record not found."))
        movementDao.deleteMovement(movement)
        return Result.success(Unit)
    }

    fun getDashboardMetrics(): Flow<DashboardMetrics> {
        val startOfToday = TimeUtils.getStartOfDay()
        val endOfToday = TimeUtils.getEndOfDay()

        val totalPersonsFlow = personDao.getTotalPersonsCount()
        val insideCountFlow = personDao.getCountByStatus("INSIDE")
        val outsideCountFlow = personDao.getCountByStatus("OUTSIDE")
        val todayOutCountFlow = movementDao.countMovementsByTypeAndRange("OUT", startOfToday, endOfToday)
        val todayInCountFlow = movementDao.countMovementsByTypeAndRange("IN", startOfToday, endOfToday)
        val todayCompletedDurationFlow = movementDao.sumDurationMinutesInRange(startOfToday, endOfToday)
        val outsidePersonsFlow = personDao.getPersonsByStatus("OUTSIDE")

        return combine(
            totalPersonsFlow,
            insideCountFlow,
            outsideCountFlow,
            todayOutCountFlow,
            todayInCountFlow
        ) { total, inside, outside, outToday, inToday ->
            Tuple5(total, inside, outside, outToday, inToday)
        }.combine(
            combine(todayCompletedDurationFlow, outsidePersonsFlow) { completedMinutes, outsideList ->
                Pair(completedMinutes, outsideList)
            }
        ) { t5, pair ->
            DashboardMetrics(
                totalPersons = t5.first,
                currentlyInside = t5.second,
                currentlyOutside = t5.third,
                todayOutRecords = t5.fourth,
                todayInRecords = t5.fifth,
                todayCompletedDurationMinutes = pair.first,
                currentlyOutsidePersons = pair.second
            )
        }
    }

    suspend fun getMovementsBetweenDirect(start: Long, end: Long): List<MovementWithPerson> {
        return movementDao.getMovementsBetweenDirect(start, end)
    }

    suspend fun getLatestMovementForPerson(personId: Long): MovementEntity? {
        return movementDao.getLatestMovementForPerson(personId)
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

data class DashboardMetrics(
    val totalPersons: Int = 0,
    val currentlyInside: Int = 0,
    val currentlyOutside: Int = 0,
    val todayOutRecords: Int = 0,
    val todayInRecords: Int = 0,
    val todayCompletedDurationMinutes: Long = 0L,
    val currentlyOutsidePersons: List<PersonEntity> = emptyList()
)
