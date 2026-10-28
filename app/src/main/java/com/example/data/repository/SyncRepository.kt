package com.example.data.repository

import com.example.data.local.database.MovementDatabase
import com.example.data.local.preferences.AppPreferences
import com.example.data.remote.api.MovementApiService
import com.example.data.remote.dto.RemoteMovementDto
import com.example.data.remote.dto.RemotePersonDto
import com.example.data.remote.dto.SyncUploadPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncRepository(
    private val database: MovementDatabase,
    private val appPreferences: AppPreferences
) {
    private val personDao = database.personDao()
    private val movementDao = database.movementDao()
    private val syncQueueDao = database.syncQueueDao()

    val pendingSyncCount: Flow<Int> = syncQueueDao.getPendingCount()

    suspend fun performSync(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = appPreferences.backendUrlFlow.first()
            if (baseUrl.isBlank() || baseUrl.contains("example.com")) {
                return@withContext Result.failure(
                    IllegalStateException("Online sync requires a configured API URL in Settings.")
                )
            }

            val api = MovementApiService.create(baseUrl)

            // 1. Gather all pending movements and persons
            val pendingQueue = syncQueueDao.getPendingQueueDirect()
            if (pendingQueue.isEmpty()) {
                val timestamp = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
                appPreferences.updateLastSync(timestamp)
                return@withContext Result.success("All records already up-to-date.")
            }

            // Prepare payload
            val personsToUpload = mutableListOf<RemotePersonDto>()
            val movementsToUpload = mutableListOf<RemoteMovementDto>()

            for (item in pendingQueue) {
                if (item.entityType == "PERSON") {
                    val p = personDao.getPersonByIdDirect(item.recordId)
                    if (p != null) {
                        personsToUpload.add(
                            RemotePersonDto(
                                id = p.id,
                                personCode = p.personCode,
                                personName = p.personName,
                                designation = p.designation,
                                department = p.department,
                                mobile = p.mobile,
                                address = p.address,
                                notes = p.notes,
                                isActive = p.isActive,
                                currentStatus = p.currentStatus
                            )
                        )
                    }
                } else if (item.entityType == "MOVEMENT") {
                    val m = movementDao.getMovementById(item.recordId)
                    if (m != null) {
                        val p = personDao.getPersonByIdDirect(m.personId)
                        val code = p?.personCode ?: "UNKNOWN"
                        movementsToUpload.add(
                            RemoteMovementDto(
                                id = m.id,
                                uuid = m.uuid,
                                personCode = code,
                                movementType = m.movementType,
                                purpose = m.purpose,
                                destination = m.destination,
                                remarks = m.remarks,
                                movementTimestamp = m.movementTimestamp,
                                durationMinutes = m.durationMinutes,
                                recordedBy = m.recordedBy
                            )
                        )
                    }
                }
            }

            val payload = SyncUploadPayload(
                persons = personsToUpload,
                movements = movementsToUpload
            )

            val uploadResponse = api.uploadSync(payload)
            if (uploadResponse.isSuccessful && uploadResponse.body()?.success == true) {
                // Mark queue items as synced
                syncQueueDao.clearCompleted()
                for (item in pendingQueue) {
                    syncQueueDao.deleteQueueItem(item.id)
                }

                val nowFormatted = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
                appPreferences.updateLastSync(nowFormatted)
                Result.success("Synced ${personsToUpload.size} person(s) and ${movementsToUpload.size} movement(s).")
            } else {
                Result.failure(
                    Exception(uploadResponse.body()?.message ?: "Sync failed with HTTP ${uploadResponse.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
