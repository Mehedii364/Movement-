package com.example.data.repository

import com.example.data.local.database.MovementDatabase
import com.example.data.local.database.entities.UserEntity
import com.example.data.local.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow

class UserRepository(
    database: MovementDatabase,
    private val appPreferences: AppPreferences
) {
    private val userDao = database.userDao()

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val currentSession = appPreferences.currentUserFlow

    suspend fun authenticate(username: String, passwordAttempt: String): Result<UserEntity> {
        val user = userDao.getUserByUsername(username.trim())
            ?: return Result.failure(IllegalArgumentException("User '$username' not found."))

        if (user.passwordHash != passwordAttempt) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }

        // Set session
        appPreferences.setUserSession(
            username = user.username,
            role = user.role,
            fullName = user.fullName
        )
        return Result.success(user)
    }

    suspend fun switchUserQuick(user: UserEntity) {
        appPreferences.setUserSession(
            username = user.username,
            role = user.role,
            fullName = user.fullName
        )
    }

    suspend fun createUser(
        username: String,
        passwordPlain: String,
        fullName: String,
        role: String
    ): Result<Long> {
        val existing = userDao.getUserByUsername(username.trim())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Username '${username}' already taken."))
        }
        val id = userDao.insertUser(
            UserEntity(
                username = username.trim(),
                passwordHash = passwordPlain,
                fullName = fullName.trim(),
                role = role
            )
        )
        return Result.success(id)
    }

    suspend fun deleteUser(id: Long): Result<Unit> {
        userDao.deleteUser(id)
        return Result.success(Unit)
    }
}
