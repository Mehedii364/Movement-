package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.database.dao.MovementDao
import com.example.data.local.database.dao.PersonDao
import com.example.data.local.database.dao.SyncQueueDao
import com.example.data.local.database.dao.UserDao
import com.example.data.local.database.entities.MovementEntity
import com.example.data.local.database.entities.PersonEntity
import com.example.data.local.database.entities.SyncQueueEntity
import com.example.data.local.database.entities.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        PersonEntity::class,
        MovementEntity::class,
        UserEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MovementDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun movementDao(): MovementDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: MovementDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MovementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovementDatabase::class.java,
                    "movement_register_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: MovementDatabase) {
            val userDao = database.userDao()
            val personDao = database.personDao()
            val movementDao = database.movementDao()

            // 1. Initial Users
            userDao.insertUser(
                UserEntity(
                    username = "admin",
                    passwordHash = "admin123", // SHA/plain for initial operator
                    fullName = "Security Supervisor",
                    role = "ADMIN"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "operator",
                    passwordHash = "op123",
                    fullName = "Gate Operator 1",
                    role = "OPERATOR"
                )
            )
            userDao.insertUser(
                UserEntity(
                    username = "viewer",
                    passwordHash = "view123",
                    fullName = "Audit Auditor",
                    role = "VIEWER"
                )
            )

            // 2. Initial Person: Rahim Uddin (EMP-1025) as required by specification
            val now = System.currentTimeMillis()
            val rahimId = personDao.insertPerson(
                PersonEntity(
                    personName = "Rahim Uddin",
                    personCode = "EMP-1025",
                    designation = "Production Officer",
                    department = "Manufacturing",
                    mobile = "+880 1711-000001",
                    address = "Plot 12, Industrial Area",
                    notes = "Factory floor supervisor",
                    isActive = true,
                    currentStatus = "OUTSIDE",
                    createdAt = now - (3 * 3600 * 1000),
                    updatedAt = now - (90 * 60 * 1000)
                )
            )

            // Also add a couple other initial diverse staff for realistic registration preview
            val person2Id = personDao.insertPerson(
                PersonEntity(
                    personName = "Fatima Begum",
                    personCode = "EMP-1042",
                    designation = "Senior Quality Analyst",
                    department = "Quality Assurance",
                    mobile = "+880 1712-000002",
                    notes = "Lab shift in-charge",
                    isActive = true,
                    currentStatus = "INSIDE",
                    createdAt = now - (8 * 3600 * 1000),
                    updatedAt = now - (30 * 60 * 1000)
                )
            )

            val person3Id = personDao.insertPerson(
                PersonEntity(
                    personName = "Kamal Hossain",
                    personCode = "EMP-1088",
                    designation = "Logistics Coordinator",
                    department = "Supply Chain",
                    mobile = "+880 1713-000003",
                    notes = "Dispatch dock supervisor",
                    isActive = true,
                    currentStatus = "INSIDE",
                    createdAt = now - (10 * 3600 * 1000),
                    updatedAt = now - (4 * 3600 * 1000)
                )
            )

            // 3. Movement entries: Rahim Uddin OUT record as specified in prompt
            // Example OUT record: Official Work, Outside Factory, Recorded By "admin"
            val outTime = now - (90 * 60 * 1000) // 1.5 hours ago
            movementDao.insertMovement(
                MovementEntity(
                    uuid = UUID.randomUUID().toString(),
                    personId = rahimId,
                    movementType = "OUT",
                    purpose = "Official Work",
                    destination = "Outside Factory",
                    remarks = "Client meeting at head office",
                    movementTimestamp = outTime,
                    durationMinutes = null,
                    recordedBy = "Security Supervisor",
                    createdAt = outTime,
                    updatedAt = outTime,
                    syncStatus = "SYNCED"
                )
            )

            // Earlier morning movements for Fatima: IN at 08:30 AM, OUT at 12:15 PM, IN at 01:00 PM (45 mins duration)
            val morningIn = now - (6 * 3600 * 1000)
            movementDao.insertMovement(
                MovementEntity(
                    uuid = UUID.randomUUID().toString(),
                    personId = person2Id,
                    movementType = "IN",
                    purpose = "Shift Start",
                    destination = "Main Plant",
                    remarks = "Morning shift arrival",
                    movementTimestamp = morningIn,
                    durationMinutes = null,
                    recordedBy = "Gate Operator 1",
                    createdAt = morningIn,
                    updatedAt = morningIn,
                    syncStatus = "SYNCED"
                )
            )

            val lunchOut = now - (3 * 3600 * 1000)
            movementDao.insertMovement(
                MovementEntity(
                    uuid = UUID.randomUUID().toString(),
                    personId = person2Id,
                    movementType = "OUT",
                    purpose = "Lunch / Break",
                    destination = "Staff Cafeteria Annex",
                    remarks = "Lunch hour",
                    movementTimestamp = lunchOut,
                    durationMinutes = null,
                    recordedBy = "Gate Operator 1",
                    createdAt = lunchOut,
                    updatedAt = lunchOut,
                    syncStatus = "SYNCED"
                )
            )

            val lunchIn = now - (2 * 3600 * 1000 + 15 * 60 * 1000)
            movementDao.insertMovement(
                MovementEntity(
                    uuid = UUID.randomUUID().toString(),
                    personId = person2Id,
                    movementType = "IN",
                    purpose = "Lunch / Break",
                    destination = "Quality Lab",
                    remarks = "Returned from lunch",
                    movementTimestamp = lunchIn,
                    durationMinutes = 45, // 45 minutes
                    recordedBy = "Gate Operator 1",
                    createdAt = lunchIn,
                    updatedAt = lunchIn,
                    syncStatus = "SYNCED"
                )
            )
        }
    }
}
