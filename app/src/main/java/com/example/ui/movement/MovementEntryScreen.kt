package com.example.ui.movement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.entities.PersonEntity
import com.example.domain.utils.TimeUtils
import com.example.ui.components.PersonPhotoAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MovementInGreen
import com.example.ui.theme.MovementOutAmber
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementEntryScreen(
    viewModel: MovementEntryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPersons by viewModel.allPersons.collectAsStateWithLifecycle()
    val customPurposes by viewModel.customPurposes.collectAsStateWithLifecycle()
    val customDestinations by viewModel.customDestinations.collectAsStateWithLifecycle()

    var showPersonSelectorSheet by remember { mutableStateOf(false) }
    var personSearchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentTimestamp = remember { System.currentTimeMillis() }
    val isOut = uiState.movementType == "OUT"

    val combinedPurposes = remember(customPurposes) {
        viewModel.basePurposes + customPurposes
    }
    val combinedDestinations = remember(customDestinations) {
        viewModel.baseDestinations + customDestinations
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("movement_entry_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title
        item {
            Column {
                Text(
                    text = "Manual Movement Entry",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Record entry or exit with timestamp, purpose, and destination",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Movement Type Selector: OUT vs IN
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isOutSelected = uiState.movementType == "OUT"
                    Surface(
                        onClick = { viewModel.setMovementType("OUT") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOutSelected) MovementOutAmber else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movement_type_out")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (isOutSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "OUT Entry",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isOutSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Leaving premises",
                                    fontSize = 10.sp,
                                    color = if (isOutSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val isInSelected = uiState.movementType == "IN"
                    Surface(
                        onClick = { viewModel.setMovementType("IN") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInSelected) MovementInGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movement_type_in")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = if (isInSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "IN Entry",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isInSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Returning to premises",
                                    fontSize = 10.sp,
                                    color = if (isInSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Person Card / Selector
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Selected Person",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showPersonSelectorSheet = true },
                        modifier = Modifier.testTag("change_person_button")
                    ) {
                        Text("Select / Change")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val person = uiState.selectedPerson
                if (person != null) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_person_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PersonPhotoAvatar(
                                photoPath = person.photoPath,
                                name = person.personName,
                                size = 52.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.personName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ID: ${person.personCode} • ${person.designation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Department: ${person.department}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                StatusBadge(status = person.currentStatus)
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPersonSelectorSheet = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tap to select a registered person", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Movement Validation Alert Banner
        item {
            val person = uiState.selectedPerson
            if (person != null) {
                if (uiState.movementType == "OUT" && person.currentStatus == "OUTSIDE") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Validation Block: ${person.personName} is already OUTSIDE. A second OUT entry cannot be recorded before returning.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (uiState.movementType == "IN" && person.currentStatus == "INSIDE") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Validation Block: ${person.personName} is already INSIDE. An IN entry cannot be recorded without an OUT movement.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Purpose Section
        item {
            Column {
                Text(
                    text = "2. Purpose of Movement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(combinedPurposes) { p ->
                        FilterChip(
                            selected = uiState.purpose == p,
                            onClick = { viewModel.setPurpose(p) },
                            label = { Text(p, fontSize = 12.sp) }
                        )
                    }
                }

                if (uiState.purpose == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.customPurpose,
                        onValueChange = viewModel::setCustomPurpose,
                        label = { Text("Specify Custom Purpose") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_purpose_input"),
                        singleLine = true
                    )
                }
            }
        }

        // Destination Section
        item {
            Column {
                Text(
                    text = "3. Destination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(combinedDestinations) { d ->
                        FilterChip(
                            selected = uiState.destination == d,
                            onClick = { viewModel.setDestination(d) },
                            label = { Text(d, fontSize = 12.sp) }
                        )
                    }
                }

                if (uiState.destination == "Custom Location") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.customDestination,
                        onValueChange = viewModel::setCustomDestination,
                        label = { Text("Enter Custom Destination") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_destination_input"),
                        singleLine = true
                    )
                }
            }
        }

        // Remarks (Optional)
        item {
            Column {
                Text(
                    text = "4. Remarks (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.remarks,
                    onValueChange = viewModel::setRemarks,
                    placeholder = { Text("e.g. Accompanied by visitor, client meeting gate pass") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("movement_remarks_input"),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        // Auto Generated Timestamp & Officer
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Timestamp (Auto):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeUtils.formatDateTime(System.currentTimeMillis()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recorded By:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.recordedBy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Messages
        item {
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { msg ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.successMessage != null) {
                uiState.successMessage?.let { msg ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MovementInGreen.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MovementInGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = MovementInGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Submit Button
        item {
            val person = uiState.selectedPerson
            val isInvalidState = person != null && (
                    (isOut && person.currentStatus == "OUTSIDE") ||
                    (!isOut && person.currentStatus == "INSIDE")
            )

            Button(
                onClick = viewModel::submitMovement,
                enabled = !uiState.isSubmitting && !isInvalidState && person != null,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOut) MovementOutAmber else MovementInGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_movement_button")
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isOut) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOut) "Save OUT Movement" else "Save IN Movement",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet to search & select person
    if (showPersonSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPersonSelectorSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Select Registered Person",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = personSearchQuery,
                    onValueChange = { personSearchQuery = it },
                    placeholder = { Text("Search by name, ID or department...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filtered = allPersons.filter {
                    personSearchQuery.isBlank() ||
                            it.personName.contains(personSearchQuery, ignoreCase = true) ||
                            it.personCode.contains(personSearchQuery, ignoreCase = true) ||
                            it.department.contains(personSearchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { p ->
                        Card(
                            onClick = {
                                viewModel.selectPerson(p)
                                showPersonSelectorSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PersonPhotoAvatar(photoPath = p.photoPath, name = p.personName, size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = p.personName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${p.personCode} • ${p.department} • ${p.designation}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = p.currentStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}
