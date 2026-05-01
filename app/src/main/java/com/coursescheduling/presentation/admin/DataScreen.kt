package com.coursescheduling.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Log
import com.coursescheduling.domain.model.LecturerWithCourses
import com.coursescheduling.theme.*

data class DataUiState(
    val lecturersWithCourses: List<LecturerWithCourses> = emptyList(),
    // Add dummy or actual lists for other categories later
    val isLoading: Boolean       = false,
    val isAdmin: Boolean         = false,
    val isFirebaseReady: Boolean = false,
    val importedFileCount: Int   = 0,
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    uiState: DataUiState,
    onImportClick: () -> Unit,
    onPushToCloudClick: () -> Unit,
    onLecturerCalendarClick: (String) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onImportFile: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        Log.d("DATA_SCREEN_TRACE", "DATA_SCREEN_ENTERED")
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            Log.d("IMPORT_TRACE", "Picker returned URI: $uri")
            uri?.let { onImportFile(it) } ?: Log.d("IMPORT_TRACE", "Picker cancelled or returned null")
        }
    )

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSnackbarDismiss()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.isAdmin) {
                FloatingActionButton(
                    onClick = { 
                        val mimeTypes = arrayOf(
                            "text/csv", 
                            "application/vnd.ms-excel", 
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/octet-stream"
                        )
                        Log.d("IMPORT_TRACE", "Launching picker with MIME types: ${mimeTypes.joinToString()}")
                        launcher.launch(mimeTypes)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "Import data", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Schedule Data",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${uiState.lecturersWithCourses.size} lecturers · " +
                                    "${uiState.lecturersWithCourses.sumOf { it.courses.size }} courses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Push to Cloud (Admin + Firebase)
                    if (uiState.isAdmin && uiState.isFirebaseReady) {
                        FilledTonalButton(
                            onClick = onPushToCloudClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Indigo100,
                                contentColor   = Indigo700
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Push", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // Tabs
                var selectedTabIndex by remember { mutableStateOf(0) }
                val tabs = listOf("Lecturers", "Classes", "Departments", "Courses", "Classrooms")

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (selectedTabIndex == 0) {
                    if (uiState.lecturersWithCourses.isEmpty()) {
                        EmptyDataState(uiState.isAdmin)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.lecturersWithCourses.forEach { lecturerWithCourses ->
                                LecturerCard(
                                    lecturerWithCourses = lecturerWithCourses,
                                    onCalendarClick = { onLecturerCalendarClick(lecturerWithCourses.lecturer.lecturerId) }
                                )
                            }
                        }
                    }
                } else {
                    // Placeholder for other tabs
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Management list for ${tabs[selectedTabIndex]} goes here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDataState(isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(40.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.FolderOff,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No data imported yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap + to import a schedule file\n(.txt, .csv or .xlsx)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LecturerCard(
    lecturerWithCourses: LecturerWithCourses,
    onCalendarClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val lecturer = lecturerWithCourses.lecturer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Lecturer header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lecturer.lecturerName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        lecturer.lecturerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${lecturerWithCourses.courses.size} course${if (lecturerWithCourses.courses.size != 1) "s" else ""} · ${lecturer.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Admin metadata
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (lecturer.mustChangePassword) {
                            Text(
                                "Temporary Pass",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                "Secure Pass",
                                style = MaterialTheme.typography.labelSmall,
                                color = AvailableGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text("•", color = Grey400)
                        Text(
                            "Updated: ${java.text.SimpleDateFormat("dd/MM/yy").format(java.util.Date(lecturer.updatedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Grey400
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Courses (expanded)
            if (expanded && lecturerWithCourses.courses.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                lecturerWithCourses.courses.forEachIndexed { index, course ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(course.courseName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${course.courseCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < lecturerWithCourses.courses.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 36.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCalendarClick) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View Calendar")
                    }
                }
            }
        }
    }
}
