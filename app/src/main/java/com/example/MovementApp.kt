package com.example

import android.app.Application
import com.example.data.export.ExcelExportManager
import com.example.data.export.ImageExportManager
import com.example.data.export.PdfExportManager
import com.example.data.local.database.MovementDatabase
import com.example.data.local.preferences.AppPreferences
import com.example.data.repository.MovementRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MovementApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { MovementDatabase.getDatabase(this, applicationScope) }
    val appPreferences by lazy { AppPreferences(this) }
    val movementRepository by lazy { MovementRepository(database) }
    val userRepository by lazy { UserRepository(database, appPreferences) }
    val syncRepository by lazy { SyncRepository(database, appPreferences) }

    val pdfExportManager by lazy { PdfExportManager(this) }
    val excelExportManager by lazy { ExcelExportManager(this) }
    val imageExportManager by lazy { ImageExportManager(this) }
}
