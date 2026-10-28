package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.database.entities.PersonEntity
import com.example.domain.utils.TimeUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class PdfExportManager(private val context: Context) {

    data class ReportMetadata(
        val title: String,
        val subtitle: String,
        val dateRangeDescription: String,
        val targetPerson: PersonEntity? = null
    )

    fun generatePdf(
        metadata: ReportMetadata,
        movements: List<MovementWithPerson>
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595 // Standard A4 width in postscript points (72 dpi)
        val pageHeight = 842 // Standard A4 height
        val margin = 36f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(26, 35, 126) // Deep Indigo
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(95, 102, 118)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(26, 35, 126)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(220, 224, 233)
            strokeWidth = 0.8f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(240, 243, 250)
            style = Paint.Style.FILL
        }

        val inBadgePaint = Paint().apply {
            color = Color.rgb(46, 125, 50)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val outBadgePaint = Paint().apply {
            color = Color.rgb(230, 81, 0)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = margin + 20f

        // Draw Document Header
        fun drawHeader(c: Canvas) {
            c.drawText("MOVEMENT — PERSON MOVEMENT REGISTER", margin, y, titlePaint)
            y += 14f
            c.drawText(metadata.title, margin, y, subtitlePaint)
            y += 12f
            c.drawText("Generated: ${TimeUtils.formatDateTime(System.currentTimeMillis())} | Period: ${metadata.dateRangeDescription}", margin, y, subtitlePaint)
            y += 16f
            c.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 12f

            // If for a specific person, draw person summary card
            metadata.targetPerson?.let { p ->
                c.drawRect(margin, y, pageWidth - margin, y + 42f, headerBgPaint)
                val cardY = y + 14f
                c.drawText("Person: ${p.personName} (${p.personCode})", margin + 10f, cardY, boldTextPaint)
                c.drawText("Department: ${p.department} | Designation: ${p.designation}", margin + 10f, cardY + 14f, textPaint)
                c.drawText("Current Status: ${p.currentStatus}", pageWidth - margin - 140f, cardY, boldTextPaint)
                y += 52f
            }

            // Draw Table Header
            c.drawRect(margin, y, pageWidth - margin, y + 20f, headerBgPaint)
            val thY = y + 14f
            c.drawText("#", margin + 6f, thY, headerPaint)
            c.drawText("Person / ID", margin + 28f, thY, headerPaint)
            c.drawText("Type", margin + 160f, thY, headerPaint)
            c.drawText("Purpose", margin + 195f, thY, headerPaint)
            c.drawText("Destination", margin + 270f, thY, headerPaint)
            c.drawText("Date & Time", margin + 355f, thY, headerPaint)
            c.drawText("Duration", margin + 440f, thY, headerPaint)
            c.drawText("By", margin + 490f, thY, headerPaint)

            y += 24f
            c.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 8f
        }

        drawHeader(canvas)

        // Iterate through rows
        for ((index, item) in movements.withIndex()) {
            if (y > pageHeight - margin - 30f) {
                // Finish page and start new one
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 20f
                drawHeader(canvas)
            }

            val rowY = y + 10f
            canvas.drawText("${index + 1}", margin + 6f, rowY, textPaint)

            val personLabel = "${item.person.personName} (${item.person.personCode})"
            val truncatedName = if (personLabel.length > 22) personLabel.take(20) + ".." else personLabel
            canvas.drawText(truncatedName, margin + 28f, rowY, boldTextPaint)

            val isOut = item.movement.movementType == "OUT"
            val badgePaint = if (isOut) outBadgePaint else inBadgePaint
            canvas.drawText(item.movement.movementType, margin + 160f, rowY, badgePaint)

            val truncatedPurpose = if (item.movement.purpose.length > 15) item.movement.purpose.take(13) + ".." else item.movement.purpose
            canvas.drawText(truncatedPurpose, margin + 195f, rowY, textPaint)

            val truncatedDest = if (item.movement.destination.length > 16) item.movement.destination.take(14) + ".." else item.movement.destination
            canvas.drawText(truncatedDest, margin + 270f, rowY, textPaint)

            val timeStr = TimeUtils.formatDateTime(item.movement.movementTimestamp, "dd/MM hh:mm a")
            canvas.drawText(timeStr, margin + 355f, rowY, textPaint)

            val durStr = if (item.movement.durationMinutes != null && item.movement.durationMinutes > 0) {
                TimeUtils.formatDuration(item.movement.durationMinutes)
            } else if (isOut) {
                "Ongoing"
            } else {
                "—"
            }
            canvas.drawText(durStr, margin + 440f, rowY, textPaint)

            val recorder = if (item.movement.recordedBy.length > 8) item.movement.recordedBy.take(7) + ".." else item.movement.recordedBy
            canvas.drawText(recorder, margin + 490f, rowY, textPaint)

            y += 18f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 4f
        }

        // Draw summary section at bottom if room or new page
        if (y > pageHeight - margin - 60f) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = margin + 20f
        }

        y += 10f
        canvas.drawRect(margin, y, pageWidth - margin, y + 36f, headerBgPaint)
        val outCount = movements.count { it.movement.movementType == "OUT" }
        val inCount = movements.count { it.movement.movementType == "IN" }
        val totalDuration = movements.mapNotNull { it.movement.durationMinutes }.sum()

        val summaryText = "Total Movements: ${movements.size} | OUT Count: $outCount | IN Count: $inCount | Completed Outside Duration: ${TimeUtils.formatDuration(totalDuration)}"
        canvas.drawText(summaryText, margin + 12f, y + 22f, boldTextPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "movement_report_${System.currentTimeMillis()}.pdf"
        val outputFile = File(exportDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    fun getShareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
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
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
