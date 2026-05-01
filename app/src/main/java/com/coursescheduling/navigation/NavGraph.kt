package com.coursescheduling.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coursescheduling.data.AuthManager
import com.coursescheduling.domain.model.Role
import com.coursescheduling.presentation.CourseViewModel
import com.coursescheduling.presentation.UiState
import com.coursescheduling.presentation.auth.*
import com.coursescheduling.presentation.settings.SettingsScreen
import com.coursescheduling.presentation.settings.SettingsViewModel
import com.coursescheduling.presentation.HomeScreen
import com.coursescheduling.presentation.HomeUiState
import com.coursescheduling.presentation.lecturer.CalendarScreen
import com.coursescheduling.presentation.lecturer.CalendarUiState
import com.coursescheduling.presentation.admin.DataScreen
import com.coursescheduling.presentation.admin.DataUiState
import com.coursescheduling.presentation.components.ImportSummaryDialog
import com.coursescheduling.presentation.components.ImportConflictDialog
import com.coursescheduling.presentation.components.MissingFieldsResolutionDialog
import com.coursescheduling.data.ImportResolution
import kotlinx.coroutines.launch
import com.coursescheduling.navigation.RequestListSection

data class NavTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false,
    val requiresAuth: Boolean = false
)

val ALL_TABS = listOf(
    NavTab(Screen.Home,     "Home",     Icons.Default.Home,         requiresAuth = true),
    NavTab(Screen.Calendar, "Calendar", Icons.Default.CalendarMonth, requiresAuth = true),
    NavTab(Screen.Data,     "Data",     Icons.Default.FolderOpen, adminOnly = true, requiresAuth = true),
    NavTab(Screen.Settings, "Settings", Icons.Default.Settings, requiresAuth = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleApp() {
    val context       = LocalContext.current
    val authManager   = remember { AuthManager.getInstance(context) }
    val courseViewModel: CourseViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val navController = rememberNavController()
    val scope         = rememberCoroutineScope()
    
    val currentUser by authManager.currentUserFlow.collectAsState()
    val userRole     = currentUser?.role

    val visibleTabs = remember(userRole, currentUser) {
        ALL_TABS.filter { tab ->
            if (tab.requiresAuth && currentUser == null) false
            else if (tab.adminOnly) userRole == Role.ADMIN
            else if (tab.screen == Screen.Calendar && userRole == Role.ADMIN) false
            else true
        }
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route, Screen.Calendar.route, Screen.Data.route, Screen.Settings.route
    )

    val allCourses by courseViewModel.allCourses.collectAsState()

    fun buildHomeState(): HomeUiState {
        return when {
            currentUser == null -> HomeUiState.Guest
            currentUser!!.role == Role.ADMIN -> HomeUiState.Admin(
                user           = currentUser!!,
                totalCourses   = allCourses.size,
                totalLecturers = allCourses.map { it.lecturerName }.distinct().size
            )
            else -> HomeUiState.Lecturer(
                user                 = currentUser!!,
                courses              = allCourses.filter { it.lecturerName == currentUser!!.fullName },
                showFirstLoginWarning = currentUser!!.isFirstLogin
            )
        }
    }

    val uiState by courseViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            Toast.makeText(context, (uiState as UiState.Error).message, Toast.LENGTH_SHORT).show()
            courseViewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    visibleTabs.forEach { tab ->
                        val selected = navBackStack?.destination?.hierarchy?.any {
                            it.route == tab.screen.route
                        } == true

                        val allRequests by courseViewModel.allRequests.collectAsState()
                        val pendingCount = allRequests.count { it.status == "PENDING" }

                        NavigationBarItem(
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (tab.screen == Screen.Calendar && userRole == Role.ADMIN && pendingCount > 0) {
                                            Badge { Text(pendingCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.SignIn.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.SignIn.route) {
                var loginState by remember { mutableStateOf(LoginUiState()) }
                LoginScreen(
                    uiState = loginState,
                    onEmailChange = { loginState = loginState.copy(email = it, error = null) },
                    onPasswordChange = { loginState = loginState.copy(password = it, error = null) },
                    onSignInClick = {
                        scope.launch {
                            loginState = loginState.copy(isLoading = true)
                            val localUser = authManager.signIn(loginState.email.trim(), loginState.password)
                            
                            if (localUser != null) {
                                loginState = loginState.copy(isLoading = false)
                                
                                if (localUser.role == Role.LECTURER && localUser.mustChangePassword) {
                                    navController.navigate(Screen.ChangePassword.route) {
                                        popUpTo(Screen.SignIn.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.SignIn.route) { inclusive = true }
                                    }
                                }
                            } else {
                                loginState = loginState.copy(isLoading = false, error = "Invalid credentials")
                            }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }

            composable(Screen.SignUp.route) {
                var signUpState by remember { mutableStateOf(SignUpUiState()) }
                SignUpScreen(
                    uiState = signUpState,
                    onUsernameChange = { signUpState = signUpState.copy(username = it) },
                    onEmailChange = { signUpState = signUpState.copy(email = it) },
                    onPasswordChange = { signUpState = signUpState.copy(password = it) },
                    onConfirmPasswordChange = { signUpState = signUpState.copy(confirmPassword = it) },
                    onRoleChange = { signUpState = signUpState.copy(selectedRole = it) },
                    onDeptChange = { signUpState = signUpState.copy(selectedDept = it) },
                    onSignUpClick = {
                        scope.launch {
                            signUpState = signUpState.copy(isLoading = true)
                            val success = authManager.signUp(
                                signUpState.username,
                                signUpState.email, signUpState.password, signUpState.selectedRole, signUpState.selectedDept
                            )
                            if (success) {
                                signUpState = signUpState.copy(isLoading = false)
                                navController.navigate(Screen.SignIn.route) {
                                    popUpTo(Screen.SignUp.route) { inclusive = true }
                                }
                            } else {
                                signUpState = signUpState.copy(isLoading = false, error = "Sign up failed")
                            }
                        }
                    },
                    onNavigateToSignIn = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = buildHomeState(),
                    onSignInClick = { navController.navigate(Screen.SignIn.route) },
                    onLogoutClick = {
                        courseViewModel.logout(navController)
                    },
                    onImportDataClick = { navController.navigate(Screen.Data.route) },
                    onViewScheduleClick = { navController.navigate(Screen.Calendar.route) },
                    onManageLecturersClick = { navController.navigate(Screen.Data.route) },
                    onManageClassroomsClick = { navController.navigate(Screen.Data.route) },
                    onResetDatabaseClick = {
                        courseViewModel.resetDatabase()
                        navController.navigate(Screen.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Calendar.route) { 
                LaunchedEffect(Unit) {
                    courseViewModel.setCalendarLecturer(null)
                    courseViewModel.observeRequests()
                }
                val calState by courseViewModel.calendarUiState.collectAsState()
                val allRequests by courseViewModel.allRequests.collectAsState()
                
                Column {
                    if (userRole == Role.ADMIN) {
                        RequestListSection(
                            requests = allRequests.filter { it.status == "PENDING" },
                            onAction = { req, approve -> courseViewModel.handleRequestAction(req, approve) }
                        )
                    }
                    CalendarScreen(
                        uiState = calState, 
                        onSlotToggle = { day, slot -> courseViewModel.onSlotToggle(day, slot) },
                        onSaveAvailability = { courseViewModel.saveAvailability() },
                        onRequestChange = { day, slot, note, type ->
                            courseViewModel.submitScheduleRequest(day, slot, note, type)
                        }
                    ) 
                }
            }

            composable(Screen.LecturerCalendar.route) { backStackEntry ->
                val lecturerId = backStackEntry.arguments?.getString("lecturerId")
                LaunchedEffect(lecturerId) {
                    courseViewModel.setCalendarLecturer(lecturerId)
                }
                val calState by courseViewModel.calendarUiState.collectAsState()
                CalendarScreen(
                    uiState = calState,
                    onSlotToggle = { day, slot -> courseViewModel.onSlotToggle(day, slot) },
                    onSaveAvailability = { courseViewModel.saveAvailability() },
                    onRequestChange = { day, slot, note, type ->
                        courseViewModel.submitScheduleRequest(day, slot, note, type)
                    }
                )
            }

            composable(Screen.Data.route) { 
                val lecturers by courseViewModel.lecturersWithCourses.collectAsState()
                val vmUiState by courseViewModel.uiState.collectAsState()

                DataScreen(
                    uiState = DataUiState(
                        lecturersWithCourses = lecturers,
                        isLoading = vmUiState is UiState.Loading,
                        isAdmin = userRole == Role.ADMIN,
                        isFirebaseReady = false
                    ),
                    onImportClick = { /* FAB handles this internally */ },
                    onPushToCloudClick = { /* Not applicable in local-only */ },
                    onLecturerCalendarClick = { id -> 
                        navController.navigate(Screen.LecturerCalendar.createRoute(id))
                    },
                    onSnackbarDismiss = { /* courseViewModel.clearMessage() */ },
                    onImportFile = { uri ->
                        courseViewModel.parseImportFile(context, uri)
                    }
                )
            }

            composable(Screen.Settings.route) { 
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(
                    onConfirm = { new ->
                        scope.launch {
                            if (authManager.changePassword(new)) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.ChangePassword.route) { inclusive = true }
                                }
                            }
                        }
                    },
                    error = null,
                    isLoading = false
                )
            }
        }

        // Global Dialogs / Overlays
        val importSummary by courseViewModel.importSummary.collectAsState()
        val pendingConflicts by courseViewModel.pendingConflicts.collectAsState()
        val missingResolutions by courseViewModel.missingFieldResolutions.collectAsState()
        val vmUiState by courseViewModel.uiState.collectAsState()
        
        importSummary?.let { summary ->
            val coursesWithMissing = summary.parsedCourses.filter { !it.isInvalid && it.missingFields.isNotEmpty() }
            val missingResolved = coursesWithMissing.all { course ->
                val res = missingResolutions[course.hashCode()] ?: emptyMap()
                course.missingFields.all { field -> res.containsKey(field) }
            }

            when {
                !missingResolved -> {
                    MissingFieldsResolutionDialog(
                        summary = summary,
                        resolutions = missingResolutions,
                        onResolve = { course: com.coursescheduling.utils.parser.ParsedCourse, field: String, value: String -> 
                            courseViewModel.resolveMissingField(course, field, value) 
                        },
                        onConfirm = { /* Will re-evaluate missingResolved and move to next step */ },
                        onCancel = { courseViewModel.clearImportSummary() }
                    )
                }
                pendingConflicts.isEmpty() -> {
                    ImportSummaryDialog(
                        summary = summary,
                        isLoading = vmUiState is UiState.Loading,
                        onConfirm = { courseViewModel.confirmImport() },
                        onCancel = { courseViewModel.clearImportSummary() }
                    )
                }
                else -> {
                    ImportConflictDialog(
                        conflicts = pendingConflicts,
                        onResolve = { conflict: com.coursescheduling.data.ImportConflict, res: com.coursescheduling.data.ImportResolution -> 
                            courseViewModel.resolveConflict(conflict, res) 
                        },
                        onConfirm = { courseViewModel.confirmImport() },
                        onCancel = { courseViewModel.clearImportSummary() }
                    )
                }
            }
        }
    }
}
