package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.database.entities.MovementWithPerson
import com.example.domain.utils.TimeUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class ExcelExportManager(private val context: Context) {

    fun generateExcelCsv(
        title: String,
        movements: List<MovementWithPerson>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "movement_export_${System.currentTimeMillis()}.csv"
        val outputFile = File(exportDir, fileName)

        FileOutputStream(outputFile).use { fos ->
            // Write UTF-8 BOM so Excel opens characters without corruption
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Title & generation header
                writer.appendLine("\"MOVEMENT REGISTER — $title\"")
                writer.appendLine("\"Exported: ${TimeUtils.formatDateTime(System.currentTimeMillis())}\"")
                writer.appendLine()

                // Columns
                val headers = listOf(
                    "Serial Number",
                    "Person Name",
                    "Person ID",
                    "Department",
                    "Movement Type",
                    "Purpose",
                    "Destination",
                    "Date",
                    "Time",
                    "Outside Duration",
                    "Remarks",
                    "Recorded By"
                )
                writer.appendLine(headers.joinToString(",") { "\"$it\"" })

                // Rows
                for ((idx, item) in movements.withIndex()) {
                    val m = item.movement
                    val p = item.person
                    val dateStr = TimeUtils.formatDate(m.movementTimestamp, "dd/MM/yyyy")
                    val timeStr = TimeUtils.formatTime(m.movementTimestamp)
                    val durationStr = if (m.durationMinutes != null && m.durationMinutes > 0) {
                        TimeUtils.formatDuration(m.durationMinutes)
                    } else if (m.movementType == "OUT") {
                        "Ongoing"
                    } else {
                        "N/A"
                    }

                    val row = listOf(
                        (idx + 1).toString(),
                        p.personName,
                        p.personCode,
                        p.department,
                        m.movementType,
                        m.purpose,
                        m.destination,
                        dateStr,
                        timeStr,
                        durationStr,
                        m.remarks.replace("\"", "\"\""),
                        m.recordedBy
                    )
                    writer.appendLine(row.joinToString(",") { "\"$it\"" })
                }

                // Summary rows
                writer.appendLine()
                writer.appendLine("\"--- SUMMARY ---\"")
                val totalOut = movements.count { it.movement.movementType == "OUT" }
                val totalIn = movements.count { it.movement.movementType == "IN" }
                val totalMinutes = movements.mapNotNull { it.movement.durationMinutes }.sum()

                writer.appendLine("\"Total Movement Records\",\"${movements.size}\"")
                writer.appendLine("\"Total OUT Count\",\"$totalOut\"")
                writer.appendLine("\"Total IN Count\",\"$totalIn\"")
                writer.appendLine("\"Total Completed Duration\",\"${TimeUtils.formatDuration(totalMinutes)}\"")
            }
        }

        return outputFile
    }

    fun getShareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getViewIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
