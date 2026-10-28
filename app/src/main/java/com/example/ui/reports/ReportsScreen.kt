package com.example.ui.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.utils.TimeUtils
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.PersonPhotoAvatar
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.Emerald
import com.example.ui.theme.MovementInGreen
import com.example.ui.theme.MovementOutAmber

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val period by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedPerson by viewModel.selectedPerson.collectAsStateWithLifecycle()
    val movements by viewModel.reportMovements.collectAsStateWithLifecycle()
    val allPersons by viewModel.allPersons.collectAsStateWithLifecycle()
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("reports_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Reports & File Export",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate and export official PDF, Excel (XLSX/CSV), or image reports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Period Selector
            item {
                Column {
                    Text(
                        text = "Report Period",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportPeriod.values()) { p ->
                            val label = when (p) {
                                ReportPeriod.TODAY -> "Today"
                                ReportPeriod.YESTERDAY -> "Yesterday"
                                ReportPeriod.LAST_7_DAYS -> "Last 7 Days"
                                ReportPeriod.THIS_MONTH -> "This Month"
                                ReportPeriod.PREVIOUS_MONTH -> "Previous Month"
                                ReportPeriod.ALL_TIME -> "All Time"
                            }
                            FilterChip(
                                selected = period == p,
                                onClick = { viewModel.setPeriod(p) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Person Selector Filter
            item {
                Column {
                    Text(
                        text = "Target Personnel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedPerson == null,
                                onClick = { viewModel.selectPerson(null) },
                                label = { Text("All Personnel", fontSize = 12.sp) }
                            )
                        }
                        items(allPersons) { p ->
                            FilterChip(
                                selected = selectedPerson?.id == p.id,
                                onClick = { viewModel.selectPerson(p) },
                                label = { Text("${p.personName} (${p.personCode})", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Summary Metrics Card
            item {
                val outCount = movements.count { it.movement.movementType == "OUT" }
                val inCount = movements.count { it.movement.movementType == "IN" }
                val totalMinutes = movements.mapNotNull { it.movement.durationMinutes }.sum()

                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedPerson != null) "Individual Report Summary: ${selectedPerson?.personName}" else "General Movement Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryMetricBox(title = "Total Records", value = "${movements.size}", modifier = Modifier.weight(1f))
                            SummaryMetricBox(title = "OUT Count", value = "$outCount", color = MovementOutAmber, modifier = Modifier.weight(1f))
                            SummaryMetricBox(title = "IN Count", value = "$inCount", color = MovementInGreen, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Outside Duration:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = TimeUtils.formatDuration(totalMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Export Actions Section
            item {
                Column {
                    Text(
                        text = "Export Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExportActionButton(
                            title = "Export PDF",
                            subtitle = "Multi-page PDF",
                            icon = Icons.Default.PictureAsPdf,
                            color = DeepIndigo,
                            onClick = viewModel::exportPdf,
                            isLoading = isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_button")
                        )

                        ExportActionButton(
                            title = "Export Excel",
                            subtitle = "CSV / Spreadsheet",
                            icon = Icons.Default.TableChart,
                            color = Emerald,
                            onClick = viewModel::exportExcel,
                            isLoading = isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExportActionButton(
                            title = "Export Image (PNG)",
                            subtitle = "Visual Card Report",
                            icon = Icons.Default.Image,
                            color = Color(0xFF6A1B9A),
                            onClick = { viewModel.exportImage(asPng = true) },
                            isLoading = isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_image_button")
                        )

                        ExportActionButton(
                            title = "Export Image (JPG)",
                            subtitle = "Compressed Photo",
                            icon = Icons.Default.Image,
                            color = Color(0xFFAD1457),
                            onClick = { viewModel.exportImage(asPng = false) },
                            isLoading = isExporting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Records Preview Table Header
            item {
                Text(
                    text = "Report Records Preview (${movements.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (movements.isEmpty()) {
                item {
                    Text(
                        text = "No movement records found for this period and personnel filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(movements.take(30)) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PersonPhotoAvatar(photoPath = item.person.photoPath, name = item.person.personName, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = item.person.personName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    MovementTypeBadge(type = item.movement.movementType)
                                }
                                Text(
                                    text = "${item.movement.purpose} → ${item.movement.destination}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = TimeUtils.formatDateTime(item.movement.movementTimestamp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.movement.durationMinutes != null && item.movement.durationMinutes > 0) {
                                Text(
                                    text = TimeUtils.formatDuration(item.movement.durationMinutes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Export Success Dialog with Share / View actions
    exportResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::clearExportResult,
            title = {
                Text(
                    text = "${result.fileType} Export Ready",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File successfully generated:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.file.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Size: ${result.file.length() / 1024} KB",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent.createChooser(result.shareIntent, "Share ${result.fileType}"))
                        } catch (_: Exception) {}
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Send")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(result.viewIntent)
                            } catch (_: Exception) {
                                // Fallback to share intent if no dedicated viewer app
                                context.startActivity(Intent.createChooser(result.shareIntent, "Open With"))
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = viewModel::clearExportResult) {
                        Text("Done")
                    }
                }
            }
        )
    }
}

@Composable
fun SummaryMetricBox(
    title: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ExportActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
