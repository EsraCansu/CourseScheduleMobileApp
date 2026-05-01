package com.coursescheduling.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coursescheduling.domain.model.Course
import com.coursescheduling.domain.model.Role
import com.coursescheduling.domain.model.User
import com.coursescheduling.theme.*

// ── State ─────────────────────────────────────────────────────────────────────
sealed class HomeUiState {
    object Guest : HomeUiState()
    data class Admin(
        val user: User,
        val totalCourses: Int     = 0,
        val totalLecturers: Int   = 0,
        val recentActivity: List<String> = emptyList()
    ) : HomeUiState()
    data class Lecturer(
        val user: User,
        val courses: List<Course> = emptyList(),
        val showFirstLoginWarning: Boolean = false
    ) : HomeUiState()
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSignInClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onImportDataClick: () -> Unit = {},
    onViewScheduleClick: () -> Unit = {},
    onManageLecturersClick: () -> Unit = {},
    onManageClassroomsClick: () -> Unit = {},
    onResetDatabaseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is HomeUiState.Guest    -> GuestView(onSignInClick, modifier)
        is HomeUiState.Admin    -> AdminDashboard(uiState, onLogoutClick, onImportDataClick, onViewScheduleClick, onManageLecturersClick, onManageClassroomsClick, onResetDatabaseClick, modifier)
        is HomeUiState.Lecturer -> LecturerDashboard(uiState, onLogoutClick, onViewScheduleClick, modifier)
    }
}

// ── Guest View ───────────────────────────────────────────────────────────────
@Composable
private fun GuestView(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Hero section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.radialGradient(listOf(Indigo400, Indigo700))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(52.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Course Scheduler",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "University scheduling made simple",
                style = MaterialTheme.typography.bodyLarge,
                color = Indigo200
            )

            Spacer(Modifier.height(40.dp))

            // Feature cards
            val features = listOf(
                Triple(Icons.Default.Groups,    "Departments", "Manage academic departments"),
                Triple(Icons.Default.MenuBook,  "Courses",     "Browse all courses"),
                Triple(Icons.Default.Person,    "Lecturers",   "View lecturer profiles"),
                Triple(Icons.Default.Schedule,  "Schedule",    "View timetables")
            )
            features.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (icon, title, subtitle) ->
                        FeatureCard(icon, title, subtitle, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.weight(1f))

            // Sign In button
            Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Login, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign In", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Grey400, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Admin Dashboard ───────────────────────────────────────────────────────────
@Composable
private fun AdminDashboard(
    state: HomeUiState.Admin,
    onLogoutClick: () -> Unit,
    onImportDataClick: () -> Unit,
    onViewScheduleClick: () -> Unit,
    onManageLecturersClick: () -> Unit,
    onManageClassroomsClick: () -> Unit,
    onResetDatabaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Welcome, ${state.user.fullName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.Logout, "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Courses", state.totalCourses.toString(), Icons.Default.MenuBook, Modifier.weight(1f))
                StatCard("Lecturers", state.totalLecturers.toString(), Icons.Default.Groups, Modifier.weight(1f))
            }
        }

        item {
            // Quick actions
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val actions = listOf(
            Triple(Icons.Default.UploadFile,      "Import Data",     "Upload CSV or Excel files"),
            Triple(Icons.Default.CalendarMonth,   "View Schedule",   "Browse full timetable"),
            Triple(Icons.Default.SupervisorAccount,"Manage Lecturers","Add or edit lecturers"),
            Triple(Icons.Default.MeetingRoom,     "Classrooms",      "Manage classrooms & types")
        )
        items(actions) { (icon, title, subtitle) ->
            ActionCard(
                icon = icon, 
                title = title, 
                subtitle = subtitle,
                onClick = { 
                    when (title) {
                        "Import Data" -> onImportDataClick()
                        "View Schedule" -> onViewScheduleClick()
                        "Manage Lecturers" -> onManageLecturersClick()
                        "Classrooms" -> onManageClassroomsClick()
                    }
                }
            )
        }

        item {
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = onResetDatabaseClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Local Database")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Indigo50),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Indigo600, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Lecturer Dashboard ────────────────────────────────────────────────────────
@Composable
private fun LecturerDashboard(
    state: HomeUiState.Lecturer,
    onLogoutClick: () -> Unit,
    onViewScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("My Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Welcome, ${state.user.fullName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.Logout, "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // First-login warning
        if (state.showFirstLoginWarning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmberBg)
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Warning, null, tint = WarningAmber, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Security Notice", fontWeight = FontWeight.SemiBold, color = WarningAmber)
                            Text(
                                "Please change your password in Settings for security.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey800
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "My Courses (${state.courses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (state.courses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.MenuBook, null, tint = Grey400, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No courses assigned", color = Grey400, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(state.courses) { course ->
                CourseCard(course)
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Indigo100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course.courseCode.take(2),
                    color = Indigo700,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(course.courseName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${course.courseCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
