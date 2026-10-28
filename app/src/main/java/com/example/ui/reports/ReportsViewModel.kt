package com.example.ui.reports

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.export.ExcelExportManager
import com.example.data.export.ImageExportManager
import com.example.data.export.PdfExportManager
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.database.entities.PersonEntity
import com.example.data.repository.MovementRepository
import com.example.domain.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ReportPeriod {
    TODAY, YESTERDAY, LAST_7_DAYS, THIS_MONTH, PREVIOUS_MONTH, ALL_TIME
}

data class ExportResult(
    val file: File,
    val shareIntent: Intent,
    val viewIntent: Intent,
    val fileType: String // "PDF", "EXCEL", "IMAGE"
)

class ReportsViewModel(
    private val movementRepository: MovementRepository,
    private val pdfExportManager: PdfExportManager,
    private val excelExportManager: ExcelExportManager,
    private val imageExportManager: ImageExportManager
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _selectedPerson = MutableStateFlow<PersonEntity?>(null)
    val selectedPerson = _selectedPerson.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult = _exportResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage = _exportMessage.asStateFlow()

    val allPersons: StateFlow<List<PersonEntity>> = movementRepository.allPersons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reportMovements: StateFlow<List<MovementWithPerson>> = combine(
        movementRepository.allMovementsWithPerson,
        _selectedPeriod,
        _selectedPerson
    ) { movements, period, person ->
        val now = System.currentTimeMillis()
        val (startTime, endTime) = when (period) {
            ReportPeriod.TODAY -> Pair(TimeUtils.getStartOfDay(now), TimeUtils.getEndOfDay(now))
            ReportPeriod.YESTERDAY -> Pair(TimeUtils.getStartOfDay(now - 86400000L), TimeUtils.getEndOfDay(now - 86400000L))
            ReportPeriod.LAST_7_DAYS -> Pair(TimeUtils.getStartOfDaysAgo(7), now)
            ReportPeriod.THIS_MONTH -> Pair(TimeUtils.getStartOfMonth(), now)
            ReportPeriod.PREVIOUS_MONTH -> Pair(TimeUtils.getStartOfPreviousMonth(), TimeUtils.getEndOfPreviousMonth())
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }

        movements.filter { item ->
            val inRange = item.movement.movementTimestamp in startTime..endTime
            val matchesPerson = person == null || item.person.id == person.id
            inRange && matchesPerson
        }.sortedByDescending { it.movement.movementTimestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun selectPerson(person: PersonEntity?) {
        _selectedPerson.value = person
    }

    fun clearExportResult() {
        _exportResult.value = null
        _exportMessage.value = null
    }

    fun exportPdf() {
        _isExporting.value = true
        _exportMessage.value = null
        viewModelScope.launch {
            try {
                val list = reportMovements.value
                val person = _selectedPerson.value
                val periodName = _selectedPeriod.value.name.replace("_", " ")

                val title = if (person != null) {
                    "Individual Movement Report - ${person.personName}"
                } else {
                    "Movement Register - $periodName"
                }

                val metadata = PdfExportManager.ReportMetadata(
                    title = title,
                    subtitle = "Official Security Movement Record",
                    dateRangeDescription = periodName,
                    targetPerson = person
                )

                val file = pdfExportManager.generatePdf(metadata, list)
                _exportResult.value = ExportResult(
                    file = file,
                    shareIntent = pdfExportManager.getShareIntent(file),
                    viewIntent = pdfExportManager.getViewIntent(file),
                    fileType = "PDF"
                )
                _exportMessage.value = "PDF report generated successfully!"
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportExcel() {
        _isExporting.value = true
        _exportMessage.value = null
        viewModelScope.launch {
            try {
                val list = reportMovements.value
                val periodName = _selectedPeriod.value.name.replace("_", " ")
                val person = _selectedPerson.value
                val title = if (person != null) "${person.personName} - $periodName" else periodName

                val file = excelExportManager.generateExcelCsv(title, list)
                _exportResult.value = ExportResult(
                    file = file,
                    shareIntent = excelExportManager.getShareIntent(file),
                    viewIntent = excelExportManager.getViewIntent(file),
                    fileType = "EXCEL"
                )
                _exportMessage.value = "Excel report created successfully!"
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportImage(asPng: Boolean = true) {
        _isExporting.value = true
        _exportMessage.value = null
        viewModelScope.launch {
            try {
                val list = reportMovements.value
                val person = _selectedPerson.value
                val periodName = _selectedPeriod.value.name.replace("_", " ")
                val title = if (person != null) person.personName else "Comprehensive Register"

                val file = imageExportManager.generateReportImage(
                    title = title,
                    dateRange = periodName,
                    person = person,
                    movements = list,
                    formatAsPng = asPng
                )
                _exportResult.value = ExportResult(
                    file = file,
                    shareIntent = imageExportManager.getShareIntent(file, asPng),
                    viewIntent = imageExportManager.getShareIntent(file, asPng), // sharing image
                    fileType = if (asPng) "PNG" else "JPG"
                )
                _exportMessage.value = "Report image generated successfully!"
            } catch (e: Exception) {
                _exportMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    class Factory(
        private val movementRepository: MovementRepository,
        private val pdfExportManager: PdfExportManager,
        private val excelExportManager: ExcelExportManager,
        private val imageExportManager: ImageExportManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(
                movementRepository,
                pdfExportManager,
                excelExportManager,
                imageExportManager
            ) as T
        }
    }
}
