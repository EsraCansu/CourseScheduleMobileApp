package com.coursescheduling.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.coursescheduling.data.ImportConflict
import com.coursescheduling.data.ConflictType
import com.coursescheduling.data.ImportResolution
import com.coursescheduling.domain.model.Course
import com.coursescheduling.domain.model.LecturerEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coursescheduling.utils.parser.ParsedCourse
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import com.coursescheduling.theme.AvailableGreen
import com.coursescheduling.utils.parser.ImportSummary

@Composable
fun ImportSummaryDialog(
    summary: ImportSummary,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onCancel() },
        title = {
            Text("Import Data Preview")
        },
        text = {
            ImportDialogContent(summary)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading && summary.validRows > 0
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Confirm Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ImportDialogContent(summary: ImportSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Statistics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("Total", summary.totalFound.toString(), MaterialTheme.colorScheme.onSurface)
                StatItem("Valid", summary.validRows.toString(), AvailableGreen)
                StatItem("Invalid", summary.invalidRows.toString(), MaterialTheme.colorScheme.error)
            }
        }

        // Preview Table
        Text("Sample Preview (Top 5 Rows):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        
        if (summary.unmappedFields.isNotEmpty()) {
            Text(
                "Unmapped: ${summary.unmappedFields.joinToString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Table Header
                Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(4.dp)) {
                    Text("Code", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Lecturer", Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Day", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                
                summary.parsedCourses.take(5).forEach { course ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.padding(4.dp)) {
                        Text(course.courseCode, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(course.lecturerName, Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                        Text(course.day, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (summary.invalidRows > 0) {
            Text(
                "⚠️ ${summary.invalidRows} rows are missing core fields (Lecturer/Code/Email) and will be skipped.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ImportConflictDialog(
    conflicts: List<ImportConflict>,
    onResolve: (ImportConflict, ImportResolution) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Resolve Import Conflicts")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "The following ${conflicts.size} items already exist in the database. Please choose an action for each.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conflicts) { conflict ->
                        ConflictItem(conflict, onResolve)
                    }
                }
            }
        },
        confirmButton = {
            val allResolved = conflicts.all { it.resolution != ImportResolution.PENDING }
            Button(
                onClick = onConfirm,
                enabled = allResolved
            ) {
                Text("Continue Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictItem(
    conflict: ImportConflict,
    onResolve: (ImportConflict, ImportResolution) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (conflict.type == ConflictType.LECTURER_CONFLICT) Icons.Default.Person else Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (conflict.type == ConflictType.LECTURER_CONFLICT) "Lecturer Conflict" else "Course Conflict",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(conflict.description, style = MaterialTheme.typography.bodySmall)
            
            // Resolution Options
            val options = listOf(ImportResolution.OVERWRITE, ImportResolution.SKIP, ImportResolution.DUPLICATE)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onResolve(conflict, option) },
                        selected = conflict.resolution == option,
                        label = {
                            Text(
                                text = when(option) {
                                    ImportResolution.OVERWRITE -> "Update"
                                    ImportResolution.SKIP -> "Skip"
                                    ImportResolution.DUPLICATE -> "Duplicate"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MissingFieldsResolutionDialog(
    summary: ImportSummary,
    resolutions: Map<Int, Map<String, String>>,
    onResolve: (ParsedCourse, String, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val coursesWithMissingFields = summary.parsedCourses.filter { !it.isInvalid && it.missingFields.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = AvailableGreen)
                Spacer(Modifier.width(8.dp))
                Text("Resolve Missing Fields")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "The following ${coursesWithMissingFields.size} courses are missing optional fields. Please provide values or defaults.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(coursesWithMissingFields) { course ->
                        MissingFieldItem(course, resolutions[course.hashCode()] ?: emptyMap(), onResolve)
                    }
                }
            }
        },
        confirmButton = {
            val allResolved = coursesWithMissingFields.all { course ->
                val res = resolutions[course.hashCode()] ?: emptyMap()
                course.missingFields.all { field -> res.containsKey(field) }
            }
            Button(
                onClick = onConfirm,
                enabled = allResolved || coursesWithMissingFields.isEmpty()
            ) {
                Text("Apply & Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MissingFieldItem(
    course: ParsedCourse,
    resolutions: Map<String, String>,
    onResolve: (ParsedCourse, String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${course.courseCode}: ${course.courseName}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            
            course.missingFields.forEach { field ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Missing $field", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    
                    if (field == "Course Day") {
                        DaySelector(
                            selectedDay = resolutions[field] ?: "",
                            onDaySelected = { onResolve(course, field, it) }
                        )
                    } else {
                        // For other fields, just a simple placeholder or "Skip"
                        TextButton(onClick = { onResolve(course, field, "TBD") }) {
                            Text("Set 'TBD'", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(if (selectedDay.isEmpty()) "Select Day" else selectedDay, style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day) },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
