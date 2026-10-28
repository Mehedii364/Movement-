package com.example.ui.persons

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.local.database.entities.PersonEntity
import com.example.domain.utils.TimeUtils
import com.example.ui.components.MovementTypeBadge
import com.example.ui.components.PersonPhotoAvatar
import com.example.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonsScreen(
    viewModel: PersonsViewModel,
    onNavigateToRecordMovementForPerson: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val persons by viewModel.filteredPersons.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val selectedPerson by viewModel.selectedPersonForDetail.collectAsStateWithLifecycle()
    val personMovements by viewModel.selectedPersonMovements.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.savePhotoFromUri(it) }
    }

    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isEditing = false
                    viewModel.prepareNewPersonForm()
                    showAddEditDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .testTag("add_person_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Person")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("persons_screen"),
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
                            text = "Person Registry",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${persons.size} registered personnel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchChanged,
                    placeholder = { Text("Search by name, ID or department...") },
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
                        .testTag("persons_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Status Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL", "INSIDE", "OUTSIDE", "INACTIVE")) { s ->
                        val label = when (s) {
                            "ALL" -> "All Personnel"
                            "INSIDE" -> "Currently Inside"
                            "OUTSIDE" -> "Currently Outside"
                            "INACTIVE" -> "Inactive"
                            else -> s
                        }
                        FilterChip(
                            selected = statusFilter == s,
                            onClick = { viewModel.onStatusFilterChanged(s) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // List of Persons
            if (persons.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No registered persons found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(persons, key = { it.id }) { person ->
                    PersonRegistryCard(
                        person = person,
                        onClick = { viewModel.selectPersonForDetail(person) },
                        onEditClick = {
                            isEditing = true
                            viewModel.prepareEditPersonForm(person)
                            showAddEditDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Person Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!formState.isSaving) showAddEditDialog = false
            },
            title = {
                Text(
                    text = if (isEditing) "Edit Person Details" else "Register New Person",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Photo selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PersonPhotoAvatar(
                            photoPath = formState.photoPath,
                            name = formState.personName.ifBlank { "User" },
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Photo", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = formState.personName,
                        onValueChange = viewModel::updateFormName,
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_form_name")
                    )

                    OutlinedTextField(
                        value = formState.personCode,
                        onValueChange = viewModel::updateFormCode,
                        label = { Text("Person ID / Employee ID *") },
                        placeholder = { Text("e.g. EMP-1025") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_form_code")
                    )

                    OutlinedTextField(
                        value = formState.designation,
                        onValueChange = viewModel::updateFormDesignation,
                        label = { Text("Designation *") },
                        placeholder = { Text("e.g. Production Officer") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_form_designation")
                    )

                    OutlinedTextField(
                        value = formState.department,
                        onValueChange = viewModel::updateFormDepartment,
                        label = { Text("Department *") },
                        placeholder = { Text("e.g. Manufacturing") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("person_form_department")
                    )

                    OutlinedTextField(
                        value = formState.mobile,
                        onValueChange = viewModel::updateFormMobile,
                        label = { Text("Mobile Number (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formState.address,
                        onValueChange = viewModel::updateFormAddress,
                        label = { Text("Address (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formState.notes,
                        onValueChange = viewModel::updateFormNotes,
                        label = { Text("Additional Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = formState.isActive,
                            onCheckedChange = viewModel::updateFormActive
                        )
                    }

                    AnimatedVisibility(visible = formState.errorMessage != null) {
                        formState.errorMessage?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.savePerson {
                            showAddEditDialog = false
                        }
                    },
                    enabled = !formState.isSaving,
                    modifier = Modifier.testTag("person_form_save_button")
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Person")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddEditDialog = false },
                    enabled = !formState.isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Person Detail & Individual Report Bottom Sheet
    selectedPerson?.let { person ->
        ModalBottomSheet(
            onDismissRequest = viewModel::clearSelectedPerson,
            sheetState = detailSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonPhotoAvatar(photoPath = person.photoPath, name = person.personName, size = 64.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = person.personName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "ID: ${person.personCode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(text = "${person.designation} • ${person.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadge(status = person.currentStatus)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Row
                val outCount = personMovements.count { it.movement.movementType == "OUT" }
                val inCount = personMovements.count { it.movement.movementType == "IN" }
                val totalMinutes = personMovements.mapNotNull { it.movement.durationMinutes }.sum()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total OUT", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$outCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total IN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$inCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1.3f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Outside Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(TimeUtils.formatDuration(totalMinutes), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Details
                Text("Contact & Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                person.mobile?.let { Text("Mobile: $it", fontSize = 13.sp) }
                person.address?.let { Text("Address: $it", fontSize = 13.sp) }
                person.notes?.let { Text("Notes: $it", fontSize = 13.sp) }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Movement History
                Text("Individual Movement History (${personMovements.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (personMovements.isEmpty()) {
                    Text("No movement entries recorded for this person yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (item in personMovements.take(15)) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MovementTypeBadge(type = item.movement.movementType)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = item.movement.purpose, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = "${item.movement.destination} • ${TimeUtils.formatDateTime(item.movement.movementTimestamp)}",
                                        fontSize = 11.sp,
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

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun PersonRegistryCard(
    person: PersonEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonPhotoAvatar(photoPath = person.photoPath, name = person.personName, size = 48.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.personName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "ID: ${person.personCode} • ${person.designation}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dept: ${person.department}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status = person.currentStatus)
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Person",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
