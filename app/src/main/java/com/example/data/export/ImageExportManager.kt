package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.database.entities.MovementWithPerson
import com.example.data.local.database.entities.PersonEntity
import com.example.domain.utils.TimeUtils
import java.io.File
import java.io.FileOutputStream

class ImageExportManager(private val context: Context) {

    fun generateReportImage(
        title: String,
        dateRange: String,
        person: PersonEntity?,
        movements: List<MovementWithPerson>,
        formatAsPng: Boolean = true
    ): File {
        val width = 1080
        val maxRows = 25 // Clean fit on a single crisp shareable image
        val rowHeight = 44
        val headerHeight = if (person != null) 360 else 240
        val footerHeight = 120
        val displayedMovements = movements.take(maxRows)
        val height = headerHeight + (displayedMovements.size * rowHeight) + footerHeight + 60

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.rgb(248, 249, 252))

        val primaryPaint = Paint().apply {
            color = Color.rgb(26, 35, 126) // Deep Indigo
            style = Paint.Style.FILL
        }

        // Top banner
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, primaryPaint)

        val bannerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("MOVEMENT REGISTER", 40f, 60f, bannerTitlePaint)

        val bannerSubtitlePaint = Paint().apply {
            color = Color.rgb(224, 230, 248)
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("Official Person Movement Report • $title", 40f, 96f, bannerSubtitlePaint)

        val metaPaint = Paint().apply {
            color = Color.rgb(90, 96, 110)
            textSize = 18f
            isAntiAlias = true
        }
        val genTime = "Generated: ${TimeUtils.formatDateTime(System.currentTimeMillis())} | Period: $dateRange"
        canvas.drawText(genTime, 40f, 160f, metaPaint)

        var currentY = 190f

        // Optional Person Card
        if (person != null) {
            val cardRect = RectF(40f, currentY, width - 40f, currentY + 130f)
            val cardPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)

            // Draw avatar circle or image
            val avatarPaint = Paint().apply {
                color = Color.rgb(63, 81, 181)
                isAntiAlias = true
            }
            canvas.drawCircle(105f, currentY + 65f, 40f, avatarPaint)

            // If photo path exists, try draw bitmap
            person.photoPath?.let { path ->
                try {
                    val photoFile = File(path)
                    if (photoFile.exists()) {
                        val photoBmp = BitmapFactory.decodeFile(path)
                        if (photoBmp != null) {
                            val destRect = Rect(65, (currentY + 25f).toInt(), 145, (currentY + 105f).toInt())
                            canvas.drawBitmap(photoBmp, null, destRect, null)
                        }
                    }
                } catch (_: Exception) {}
            }

            val namePaint = Paint().apply {
                color = Color.rgb(20, 20, 20)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("${person.personName} (${person.personCode})", 165f, currentY + 50f, namePaint)

            val detailsPaint = Paint().apply {
                color = Color.rgb(80, 80, 80)
                textSize = 18f
                isAntiAlias = true
            }
            canvas.drawText("${person.department}  •  ${person.designation}", 165f, currentY + 82f, detailsPaint)

            val statusPaint = Paint().apply {
                color = if (person.currentStatus == "INSIDE") Color.rgb(46, 125, 50) else Color.rgb(230, 81, 0)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("STATUS: ${person.currentStatus}", 165f, currentY + 110f, statusPaint)

            currentY += 155f
        }

        // Table Header
        val thRect = RectF(40f, currentY, width - 40f, currentY + 44f)
        val thBgPaint = Paint().apply {
            color = Color.rgb(232, 236, 246)
        }
        canvas.drawRoundRect(thRect, 8f, 8f, thBgPaint)

        val thTextPaint = Paint().apply {
            color = Color.rgb(26, 35, 126)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerTextY = currentY + 28f
        canvas.drawText("#", 60f, headerTextY, thTextPaint)
        canvas.drawText("Person / ID", 110f, headerTextY, thTextPaint)
        canvas.drawText("Type", 370f, headerTextY, thTextPaint)
        canvas.drawText("Purpose", 460f, headerTextY, thTextPaint)
        canvas.drawText("Destination", 630f, headerTextY, thTextPaint)
        canvas.drawText("Date & Time", 800f, headerTextY, thTextPaint)
        canvas.drawText("Duration", 950f, headerTextY, thTextPaint)

        currentY += 54f

        val rowTextPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 17f
            isAntiAlias = true
        }
        val boldRowText = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val outColorPaint = Paint().apply {
            color = Color.rgb(230, 81, 0)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val inColorPaint = Paint().apply {
            color = Color.rgb(46, 125, 50)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(225, 228, 236)
            strokeWidth = 1f
        }

        // Table Rows
        for ((idx, item) in displayedMovements.withIndex()) {
            val rY = currentY + 26f
            canvas.drawText("${idx + 1}", 60f, rY, rowTextPaint)

            val personStr = "${item.person.personName} (${item.person.personCode})"
            val truncatedPerson = if (personStr.length > 20) personStr.take(18) + ".." else personStr
            canvas.drawText(truncatedPerson, 110f, rY, boldRowText)

            val isOut = item.movement.movementType == "OUT"
            val badge = if (isOut) outColorPaint else inColorPaint
            canvas.drawText(item.movement.movementType, 370f, rY, badge)

            val pStr = if (item.movement.purpose.length > 13) item.movement.purpose.take(11) + ".." else item.movement.purpose
            canvas.drawText(pStr, 460f, rY, rowTextPaint)

            val dStr = if (item.movement.destination.length > 13) item.movement.destination.take(11) + ".." else item.movement.destination
            canvas.drawText(dStr, 630f, rY, rowTextPaint)

            val timeStr = TimeUtils.formatDateTime(item.movement.movementTimestamp, "dd/MM hh:mm a")
            canvas.drawText(timeStr, 800f, rY, rowTextPaint)

            val durStr = if (item.movement.durationMinutes != null && item.movement.durationMinutes > 0) {
                TimeUtils.formatDuration(item.movement.durationMinutes)
            } else if (isOut) {
                "Ongoing"
            } else {
                "—"
            }
            canvas.drawText(durStr, 950f, rY, rowTextPaint)

            currentY += rowHeight
            canvas.drawLine(40f, currentY, width - 40f, currentY, dividerPaint)
        }

        // Summary footer
        currentY += 20f
        val footerRect = RectF(40f, currentY, width - 40f, currentY + 65f)
        val footerBg = Paint().apply {
            color = Color.rgb(238, 240, 248)
        }
        canvas.drawRoundRect(footerRect, 10f, 10f, footerBg)

        val outCount = movements.count { it.movement.movementType == "OUT" }
        val inCount = movements.count { it.movement.movementType == "IN" }
        val totalMinutes = movements.mapNotNull { it.movement.durationMinutes }.sum()

        val summaryText = "Records: ${movements.size}   |   OUT: $outCount   |   IN: $inCount   |   Total Duration: ${TimeUtils.formatDuration(totalMinutes)}"
        val summaryPaint = Paint().apply {
            color = Color.rgb(26, 35, 126)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(summaryText, 65f, currentY + 40f, summaryPaint)

        // Save image to cache
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val extension = if (formatAsPng) "png" else "jpg"
        val fileName = "movement_report_${System.currentTimeMillis()}.$extension"
        val outputFile = File(exportDir, fileName)

        FileOutputStream(outputFile).use { out ->
            val format = if (formatAsPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            bitmap.compress(format, 95, out)
        }

        return outputFile
    }

    fun getShareIntent(file: File, isPng: Boolean): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = if (isPng) "image/png" else "image/jpeg"
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
