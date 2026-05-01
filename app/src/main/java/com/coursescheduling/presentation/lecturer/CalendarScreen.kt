package com.coursescheduling.presentation.lecturer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursescheduling.domain.model.Course
import com.coursescheduling.theme.*

enum class SlotState { AVAILABLE, UNAVAILABLE, OCCUPIED }

data class CalendarUiState(
    val slots: Map<Pair<Int, Int>, SlotState> = emptyMap(), // (dayIndex, slotIndex) → state
    val occupiedCourses: Map<Pair<Int, Int>, Course?> = emptyMap(),
    val isLecturerMode: Boolean = true,
    val canEdit: Boolean = false
)

private val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val TIME_SLOTS = listOf(
    "08:00\n09:00",
    "09:00\n10:00",
    "10:00\n11:00",
    "11:00\n12:00",
    "13:00\n14:00",
    "14:00\n15:00",
    "15:00\n16:00",
    "16:00\n17:00",
    "17:00\n18:00"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onSlotToggle: (dayIndex: Int, slotIndex: Int) -> Unit,
    onSaveAvailability: () -> Unit = {},
    onRequestChange: (dayIndex: Int, slotIndex: Int, note: String, type: String) -> Unit = {_,_,_,_ ->},
    modifier: Modifier = Modifier
) {
    var showRequestDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    if (showRequestDialog != null) {
        var note by remember { mutableStateOf("") }
        var requestType by remember { mutableStateOf("Schedule Change") }
        val dayName = DAYS[showRequestDialog!!.first]
        val slotTime = TIME_SLOTS[showRequestDialog!!.second].replace("\n", " - ")

        AlertDialog(
            onDismissRequest = { showRequestDialog = null },
            title = { Text("New Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Slot: $dayName, $slotTime", fontWeight = FontWeight.SemiBold)
                    
                    Text("Request Type", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = requestType == "Schedule Change",
                            onClick = { requestType = "Schedule Change" },
                            label = { Text("Schedule Change") }
                        )
                        FilterChip(
                            selected = requestType == "Override",
                            onClick = { requestType = "Override" },
                            label = { Text("Override") }
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRequestChange(showRequestDialog!!.first, showRequestDialog!!.second, note, requestType)
                    showRequestDialog = null
                }) { Text("Submit Request") }
            },
            dismissButton = { TextButton(onClick = { showRequestDialog = null }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (uiState.isLecturerMode) "My Availability" else "Schedule Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (uiState.isLecturerMode)
                        "Tap to toggle, Long press to request"
                    else
                        "View lecturer's calendar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.canEdit) {
                Button(
                    onClick = onSaveAvailability,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(AvailableGreen, "Available")
            LegendItem(UnavailableRed, "Unavailable")
            LegendItem(OccupiedGrey, "Occupied")
        }

        Spacer(Modifier.height(16.dp))

        // Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                // Day headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(64.dp))
                    DAYS.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                var expandedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

                // Slot rows
                TIME_SLOTS.forEachIndexed { slotIndex, slotLabel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time label
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slotLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Day cells
                        DAYS.forEachIndexed { dayIndex, _ ->
                            val key = Pair(dayIndex, slotIndex)
                            val state = uiState.slots[key] ?: SlotState.AVAILABLE
                            val course = uiState.occupiedCourses[key]
                            val isExpanded = expandedCell == key
                             GridCell(
                                 slotState = state,
                                 course = course,
                                 isClickable = true,
                                 isExpanded = isExpanded,
                                 onClick = { 
                                     if (isExpanded) {
                                         expandedCell = null
                                     } else {
                                         expandedCell = key
                                     }
                                     // Call toggle regardless of occupied state so VM can handle blocking + Toast
                                     if (uiState.canEdit) {
                                         onSlotToggle(dayIndex, slotIndex)
                                     }
                                 },
                                 onLongClick = {
                                     if (uiState.canEdit) {
                                         showRequestDialog = key
                                     }
                                 },
                                 modifier = Modifier.weight(1f)
                             )
                        }
                    }
                    if (slotIndex < TIME_SLOTS.size - 1) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(
    slotState: SlotState,
    course: Course?,
    isClickable: Boolean,
    isExpanded: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = when (slotState) {
        SlotState.AVAILABLE   -> AvailableGreen.copy(alpha = 0.15f)
        SlotState.UNAVAILABLE -> UnavailableRed.copy(alpha = 0.15f)
        SlotState.OCCUPIED    -> OccupiedGrey.copy(alpha = 0.15f)
    }
    val borderColor = when (slotState) {
        SlotState.AVAILABLE   -> AvailableGreen
        SlotState.UNAVAILABLE -> UnavailableRed
        SlotState.OCCUPIED    -> OccupiedGrey
    }
    val dotColor = borderColor

    Box(
        modifier = modifier
            .padding(3.dp)
            .height(if (isExpanded) 160.dp else 60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = isClickable
            )
            .animateContentSize(),
        contentAlignment = if (isExpanded) Alignment.TopStart else Alignment.Center
    ) {
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(6.dp).fillMaxWidth()
            ) {
                if (course != null) {
                    Text(course.courseCode, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(course.courseName, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Lecturer: Pending", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Room: TBD", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: Approved", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("No Course", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Status: ${slotState.name}", fontSize = 9.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("Notes: ...", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (course != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = course.courseCode,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = course.courseName,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
