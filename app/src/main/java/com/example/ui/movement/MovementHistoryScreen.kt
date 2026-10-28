package com.example.ui.movement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.entities.MovementWithPerson
import com.example.domain.utils.TimeUtils
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.PersonPhotoAvatar

@Composable
fun MovementHistoryScreen(
    viewModel: MovementHistoryViewModel,
    onNavigateToPersonDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val movements by viewModel.filteredMovements.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val sortNewestFirst by viewModel.sortNewestFirst.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle(null)

    val snackbarHostState = remember { SnackbarHostState() }
    var recordToCorrect by remember { mutableStateOf<MovementWithPerson?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("movement_history_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Movement History",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${movements.size} records found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = viewModel::toggleSortOrder,
                        modifier = Modifier.testTag("toggle_sort_button")
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "Toggle sort order"
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchChanged,
                    placeholder = { Text("Search by name, ID, purpose, destination...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Date Filters Row
            item {
                Column {
                    Text(
                        text = "Date Filter",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DateFilterOption.values()) { opt ->
                            val label = when (opt) {
                                DateFilterOption.ALL -> "All Dates"
                                DateFilterOption.TODAY -> "Today"
                                DateFilterOption.YESTERDAY -> "Yesterday"
                                DateFilterOption.LAST_7_DAYS -> "Last 7 Days"
                                DateFilterOption.THIS_MONTH -> "This Month"
                            }
                            FilterChip(
                                selected = dateFilter == opt,
                                onClick = { viewModel.onDateFilterChanged(opt) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Type Filter Chips: ALL, IN, OUT
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in listOf("ALL", "OUT", "IN")) {
                        FilterChip(
                            selected = typeFilter == t,
                            onClick = { viewModel.onTypeFilterChanged(t) },
                            label = { Text(if (t == "ALL") "All Types" else "$t Records", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Movement Items
            if (movements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No movement records match the criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(movements, key = { it.movement.id }) { item ->
                    MovementHistoryCard(
                        item = item,
                        canEdit = currentSession?.role == "ADMIN" || currentSession?.role == "OPERATOR",
                        onCorrectClick = { recordToCorrect = item },
                        onPersonClick = { onNavigateToPersonDetail(item.person.id) }
                    )
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

    // Correction Dialog with Audit Note
    recordToCorrect?.let { item ->
        var correctedPurpose by remember { mutableStateOf(item.movement.purpose) }
        var correctedDestination by remember { mutableStateOf(item.movement.destination) }
        var correctedRemarks by remember { mutableStateOf(item.movement.remarks) }
        var correctionReason by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { recordToCorrect = null },
            title = {
                Text(
                    text = "Correct Movement Record",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Person: ${item.person.personName} (${item.person.personCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = correctedPurpose,
                        onValueChange = { correctedPurpose = it },
                        label = { Text("Purpose") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = correctedDestination,
                        onValueChange = { correctedDestination = it },
                        label = { Text("Destination") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = correctedRemarks,
                        onValueChange = { correctedRemarks = it },
                        label = { Text("Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = correctionReason,
                        onValueChange = {
                            correctionReason = it
                            hasError = false
                        },
                        label = { Text("Reason for Correction (Required for audit)") },
                        isError = hasError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (correctionReason.isBlank()) {
                            hasError = true
                        } else {
                            viewModel.correctRecord(
                                movementId = item.movement.id,
                                newPurpose = correctedPurpose,
                                newDestination = correctedDestination,
                                newRemarks = correctedRemarks,
                                operatorName = currentSession?.fullName ?: "Operator",
                                correctionReason = correctionReason.trim()
                            )
                            recordToCorrect = null
                        }
                    }
                ) {
                    Text("Save Correction")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToCorrect = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MovementHistoryCard(
    item: MovementWithPerson,
    canEdit: Boolean,
    onCorrectClick: () -> Unit,
    onPersonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = item.movement
    val p = item.person

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Person, Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable { onPersonClick() }) {
                    PersonPhotoAvatar(photoPath = p.photoPath, name = p.personName, size = 44.dp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPersonClick() }
                ) {
                    Text(
                        text = p.personName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${p.personCode} • ${p.department}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MovementTypeBadge(type = m.movementType)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Purpose & Destination
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Purpose",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = m.purpose,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Destination",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = m.destination,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamps and Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recorded At",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = TimeUtils.formatDateTime(m.movementTimestamp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                if (m.durationMinutes != null && m.durationMinutes > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Outside: ${TimeUtils.formatDuration(m.durationMinutes)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (m.movementType == "OUT") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Duration: Ongoing",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (m.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Remarks: ${m.remarks}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Recorded By and Correction Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By: ${m.recordedBy}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (canEdit) {
                    TextButton(
                        onClick = onCorrectClick,
                        modifier = Modifier.testTag("correct_record_${m.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Correct Record",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Correct", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
