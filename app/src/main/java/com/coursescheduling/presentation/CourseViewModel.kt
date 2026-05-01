package com.coursescheduling.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coursescheduling.data.AuthManager
import com.coursescheduling.data.ConflictManager
import com.coursescheduling.data.repository.CourseRepository
import com.coursescheduling.data.repository.LecturerRepository
import com.coursescheduling.data.ScheduleMatrixManager
import com.coursescheduling.data.local.AppDatabase
import com.coursescheduling.data.ImportConflict
import com.coursescheduling.data.ConflictType
import com.coursescheduling.data.ImportResolution
import com.coursescheduling.domain.model.Course
import com.coursescheduling.domain.model.LecturerEntity
import com.coursescheduling.domain.model.LecturerWithCourses
import com.coursescheduling.domain.model.AvailabilityEntity
import com.coursescheduling.domain.model.ScheduleRequestEntity
import com.coursescheduling.domain.model.ScheduleEntity
import com.coursescheduling.domain.model.DepartmentEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.Uri
import com.coursescheduling.presentation.lecturer.CalendarUiState
import com.coursescheduling.presentation.lecturer.SlotState
import com.coursescheduling.utils.parser.ImportEngine
import com.coursescheduling.utils.parser.ImportSummary
import com.coursescheduling.utils.parser.ParsedCourse

class CourseViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val courseRepository = CourseRepository(db.courseDao())
    private val lecturerRepository = LecturerRepository(db.lecturerDao())
    private val authManager = AuthManager.getInstance(app)

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _allCourses = MutableStateFlow<List<Course>>(emptyList())
    val allCourses: StateFlow<List<Course>> = _allCourses.asStateFlow()

    private val _calendarUiState = MutableStateFlow(CalendarUiState())
    val calendarUiState: StateFlow<CalendarUiState> = _calendarUiState.asStateFlow()

    private val _tempAvailability = MutableStateFlow<Map<Pair<Int, Int>, Boolean>>(emptyMap())
    val tempAvailability: StateFlow<Map<Pair<Int, Int>, Boolean>> = _tempAvailability.asStateFlow()

    private val _currentCalendarLecturerId = MutableStateFlow<String?>(null)
    val currentCalendarLecturerId: StateFlow<String?> = _currentCalendarLecturerId.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val baseFlow = combine(
                courseRepository.observeCourses(),
                lecturerRepository.observeLecturers(),
                db.scheduleDao().getAllSchedulesFlow(),
                db.availabilityDao().getAllAvailability(),
                authManager.currentUserFlow
            ) { courses, lecturers, schedules, allAvailability, user ->
                // Intermediate object to pass down
                DataSnapshot(courses, lecturers, schedules, allAvailability, user)
            }

            combine(
                baseFlow,
                _tempAvailability,
                _currentCalendarLecturerId
            ) { snapshot, temp, targetLecturerId ->
                val (courses, lecturers, schedules, allAvailability, user) = snapshot
                
                Log.d("CALENDAR_SYNC_FIX", "admin calendar refresh trigger: Refreshing for user=${user?.fullName}, target=$targetLecturerId")
                
                _allCourses.value = courses
                _allLecturers.value = lecturers
                
                // Update matrix
                syncMatrixWithDatabase(courses)
                updateGroupedList(lecturers, courses)
                
                val targetId = targetLecturerId ?: user?.id ?: ""
                val isAdmin = user?.role == com.coursescheduling.domain.model.Role.ADMIN
                val isLecturer = user?.role == com.coursescheduling.domain.model.Role.LECTURER
                
                // Mode logic:
                // If targetLecturerId is set -> show that lecturer's calendar.
                // If targetLecturerId is null AND user is LECTURER -> show their own.
                // If targetLecturerId is null AND user is ADMIN -> show ALL (Global Mode).
                
                val showAll = isAdmin && targetLecturerId == null
                val isLecturerMode = targetLecturerId != null || isLecturer
                
                // canEdit is true only if the logged-in user is a lecturer and they are NOT viewing someone else's calendar
                val canEdit = isLecturer && (targetLecturerId == null || targetLecturerId == user?.id)
                
                Log.d("CALENDAR_SYNC_FIX", "admin calendar refresh trigger: mode=${if(showAll) "GLOBAL" else "SPECIFIC"}, target=$targetId")

                val slots = mutableMapOf<Pair<Int, Int>, SlotState>()
                val occupied = mutableMapOf<Pair<Int, Int>, Course?>()
                
                // 1. Mark Availability from DB (only if showing specific lecturer)
                if (!showAll) {
                    allAvailability.filter { it.lecturerId == targetId }.forEach { avail ->
                        val key = Pair(avail.dayIndex, avail.slotIndex)
                        slots[key] = if (avail.isAvailable) SlotState.AVAILABLE else SlotState.UNAVAILABLE
                    }
                }

                // 2. Override with Temporary Changes (OPTIMISTIC UI)
                if (!showAll) {
                    temp.forEach { (key, isAvailable) ->
                        slots[key] = if (isAvailable) SlotState.AVAILABLE else SlotState.UNAVAILABLE
                    }
                }

                // 3. Mark Occupied (Highest Priority)
                val filteredSchedules = if (showAll) schedules else schedules.filter { it.lecturerId == targetId }
                filteredSchedules.forEach { schedule ->
                    val key = Pair(schedule.weekday, schedule.timeSlotIndex)
                    slots[key] = SlotState.OCCUPIED // Priority Rule: OCCUPIED > UNAVAILABLE > AVAILABLE
                    occupied[key] = courses.find { it.id == schedule.courseId }
                }
                
                _calendarUiState.value = _calendarUiState.value.copy(
                    slots = slots,
                    occupiedCourses = occupied,
                    isLecturerMode = isLecturerMode,
                    canEdit = canEdit
                )
            }.collect()
        }
    }

    private data class DataSnapshot(
        val courses: List<Course>,
        val lecturers: List<com.coursescheduling.domain.model.LecturerEntity>,
        val schedules: List<com.coursescheduling.domain.model.ScheduleEntity>,
        val availability: List<com.coursescheduling.domain.model.AvailabilityEntity>,
        val user: com.coursescheduling.domain.model.User?
    )

    fun onSlotToggle(dayIndex: Int, slotIndex: Int) {
        val key = Pair(dayIndex, slotIndex)
        val currentState = _calendarUiState.value.slots[key] ?: SlotState.AVAILABLE
        
        if (currentState == SlotState.OCCUPIED) {
            Log.w("CALENDAR_SYNC_FIX", "occupied blocking behavior: day=$dayIndex slot=$slotIndex is OCCUPIED")
            _uiState.value = UiState.Error("Occupied slot cannot be modified")
            return
        }

        val nextAvailable = currentState != SlotState.AVAILABLE
        val currentTemp = _tempAvailability.value.toMutableMap()
        currentTemp[key] = nextAvailable
        _tempAvailability.value = currentTemp
        Log.d("CALENDAR_SYNC_FIX", "cell tap state change: day=$dayIndex slot=$slotIndex -> ${if(nextAvailable) "AVAILABLE" else "UNAVAILABLE"}")
    }

    fun saveAvailability() {
        val user = authManager.currentUserFlow.value ?: return
        val targetId = _currentCalendarLecturerId.value ?: user.id
        
        viewModelScope.launch {
            try {
                val entities = _tempAvailability.value.map { (key, isAvailable) ->
                    AvailabilityEntity(
                        lecturerId = targetId,
                        dayIndex = key.first,
                        slotIndex = key.second,
                        isAvailable = isAvailable
                    )
                }
                db.availabilityDao().insertAll(entities)
                _tempAvailability.value = emptyMap()
                Log.d("CALENDAR_SYNC_FIX", "save operation result: Success for $targetId")
            } catch (e: Exception) {
                Log.e("CALENDAR_SYNC_FIX", "save operation result: Failed - ${e.message}")
            }
        }
    }

    fun setCalendarLecturer(lecturerId: String?) {
        _currentCalendarLecturerId.value = lecturerId
        _tempAvailability.value = emptyMap() // Reset temp changes when switching
        Log.d("CALENDAR_SYNC_FIX", "lecturerId sync updates: Viewing calendar for $lecturerId")
    }

    fun resetDatabase() {
        viewModelScope.launch {
            try {
                db.clearAllTables()
                authManager.signOut()
                _allCourses.value = emptyList()
                _calendarUiState.value = CalendarUiState()
                Log.d("APP_FLOW_DEBUG", "Database manually reset by Admin")
            } catch (e: Exception) {
                Log.e("APP_FLOW_DEBUG", "Reset failed: ${e.message}")
            }
        }
    }

    private val _allLecturers = MutableStateFlow<List<LecturerEntity>>(emptyList())
    val allLecturers: StateFlow<List<LecturerEntity>> = _allLecturers.asStateFlow()

    private val _lecturersWithCourses = MutableStateFlow<List<LecturerWithCourses>>(emptyList())
    val lecturersWithCourses: StateFlow<List<LecturerWithCourses>> = _lecturersWithCourses.asStateFlow()

    private val _importSummary = MutableStateFlow<ImportSummary?>(null)
    val importSummary: StateFlow<ImportSummary?> = _importSummary.asStateFlow()

    // Map of (ParsedCourse.hashCode to (FieldName to Value))
    private val _missingFieldResolutions = MutableStateFlow<Map<Int, Map<String, String>>>(emptyMap())
    val missingFieldResolutions: StateFlow<Map<Int, Map<String, String>>> = _missingFieldResolutions.asStateFlow()

    fun resolveMissingField(course: ParsedCourse, field: String, value: String) {
        val current = _missingFieldResolutions.value.toMutableMap()
        val courseRes = current[course.hashCode()]?.toMutableMap() ?: mutableMapOf()
        courseRes[field] = value
        current[course.hashCode()] = courseRes
        _missingFieldResolutions.value = current
        Log.d("FINAL_IMPORT_DEBUG", "Resolved missing field '$field' with value '$value' for ${course.courseCode}")
    }


    private fun updateGroupedList(lecturers: List<LecturerEntity>, courses: List<Course>) {
        val coursesByLecturer = courses.groupBy { it.lecturerID }
        _lecturersWithCourses.value = lecturers.map { lecturer ->
            LecturerWithCourses(
                lecturer = lecturer,
                courses = coursesByLecturer[lecturer.lecturerId] ?: emptyList()
            )
        }
    }

    private fun handleError(e: Throwable) {
        val message = e.message ?: "Database error occurred"
        _uiState.value = UiState.Error(message)
    }

    fun replaceAll(courses: List<Course>, lecturers: List<LecturerEntity>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading()
            try {
                coroutineScope {
                    val coursesDeferred = async { courseRepository.replaceAll(courses) }
                    val lecturersDeferred = async { lecturerRepository.replaceAll(lecturers) }
                    coursesDeferred.await()
                    lecturersDeferred.await()
                }
                _uiState.value = UiState.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun parseImportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading()
            try {
                val summary = withContext(Dispatchers.IO) {
                    ImportEngine.parseFile(context, uri)
                }
                
                // Detect duplicates against local DB
                val existingCodes = _allCourses.value.map { it.courseCode }.toSet()
                val existingNames = _allCourses.value.map { it.courseName }.toSet()
                
                var duplicates = 0
                summary.parsedCourses.forEach { course ->
                    if (existingCodes.contains(course.courseCode) || existingNames.contains(course.courseName)) {
                        course.isDuplicate = true
                        duplicates++
                    }
                }
                
                _importSummary.value = summary.copy(duplicateRows = duplicates)
                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    private val _pendingConflicts = MutableStateFlow<List<ImportConflict>>(emptyList())
    val pendingConflicts: StateFlow<List<ImportConflict>> = _pendingConflicts.asStateFlow()

    fun resolveConflict(conflict: ImportConflict, resolution: ImportResolution) {
        val updated = _pendingConflicts.value.map {
            if (it === conflict) it.copy(resolution = resolution) else it
        }
        _pendingConflicts.value = updated
    }

    fun confirmImport() {
        val summary = _importSummary.value ?: return
        Log.d("FINAL_IMPORT_DEBUG", "IMPORT_STARTED - Processing ${summary.parsedCourses.size} items")
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading()
            try {
                // 1. Convert ParsedCourses to temporary Entities for conflict detection
                val tempCourses = summary.parsedCourses.filter { !it.isInvalid }.map { parsed ->
                    val resolutions = _missingFieldResolutions.value[parsed.hashCode()] ?: emptyMap()
                    val finalDay = resolutions["Course Day"] ?: parsed.day
                    val finalStartTime = resolutions["Start Time"] ?: parsed.startTime
                    
                    Course(
                        courseCode = parsed.courseCode,
                        courseName = parsed.courseName,
                        lecturerName = parsed.lecturerName,
                        courseClass = resolutions["Class"] ?: parsed.courseClass,
                        classroomType = resolutions["Room"] ?: parsed.classroomType,
                        duration = parsed.duration,
                        departmentIndex = mapDepartmentToIndex(parsed.department),
                        dayIndex = mapDayToIndex(finalDay),
                        timeSlotIndex = mapTimeToSlotIndex(finalStartTime)
                    )
                }

                val tempLecturers = summary.parsedCourses.filter { !it.isInvalid }.map { parsed ->
                    val email = parsed.email.ifBlank { "${parsed.lecturerName.lowercase().replace(" ", "")}@university.com" }
                    val hashedPassword = authManager.hashPassword(parsed.lecturerPassword)
                    Log.d("IMPORT_AUTH_DEBUG", "Hashed password for ${parsed.lecturerName}: ${hashedPassword.take(10)}...")
                    
                    LecturerEntity(
                        lecturerId = java.util.UUID.randomUUID().toString(),
                        lecturerName = parsed.lecturerName,
                        lecturerTitle = parsed.lecturerTitle,
                        email = email,
                        department = parsed.department.ifBlank { "Unknown" },
                        passwordHash = hashedPassword,
                        mustChangePassword = true
                    )
                }.distinctBy { it.email.lowercase() }

                Log.d("IMPORT_AUTH_DEBUG", "Generated ${tempLecturers.size} temporary lecturer entities for conflict check")

                // 2. Detect Conflicts
                val conflicts = ConflictManager.getAllConflicts(
                    existingCourses = _allCourses.value,
                    newCourses = tempCourses,
                    existingLecturers = _allLecturers.value,
                    newLecturers = tempLecturers
                )

                if (conflicts.isNotEmpty() && _pendingConflicts.value.isEmpty()) {
                    _pendingConflicts.value = conflicts
                    _uiState.value = UiState.Idle // Wait for Admin decision
                    return@launch
                }

                // 3. Apply Resolutions
                val finalCourses = _allCourses.value.toMutableList()
                val finalLecturers = _allLecturers.value.toMutableList()

                summary.parsedCourses.filter { !it.isInvalid }.forEach { parsed ->
                    val email = parsed.email.ifBlank { "${parsed.lecturerName.lowercase().replace(" ", "")}@university.com" }
                    
                    // Resolve Lecturer
                    val lecturerConflict = _pendingConflicts.value.find { 
                        it.type == ConflictType.LECTURER_CONFLICT && 
                        (it.newItem as LecturerEntity).email.equals(email, ignoreCase = true) 
                    }
                    
                    var lecturerId = ""
                    val resolution = lecturerConflict?.resolution ?: ImportResolution.OVERWRITE
                    
                    when (resolution) {
                        ImportResolution.OVERWRITE -> {
                            val existing = finalLecturers.find { it.email.equals(email, ignoreCase = true) }
                            if (existing != null) {
                                val updated = existing.copy(
                                    lecturerName = parsed.lecturerName,
                                    lecturerTitle = parsed.lecturerTitle,
                                    department = parsed.department.ifBlank { "Unknown" }
                                )
                                finalLecturers[finalLecturers.indexOf(existing)] = updated
                                lecturerId = existing.lecturerId
                                Log.d("IMPORT_AUTH_DEBUG", "Updated existing lecturer: ${parsed.lecturerName}")
                            } else {
                                val newId = java.util.UUID.randomUUID().toString()
                                finalLecturers.add(LecturerEntity(
                                    lecturerId = newId,
                                    lecturerName = parsed.lecturerName,
                                    lecturerTitle = parsed.lecturerTitle,
                                    email = email,
                                    department = parsed.department.ifBlank { "Unknown" },
                                    passwordHash = authManager.hashPassword(parsed.lecturerPassword),
                                    mustChangePassword = true
                                ))
                                lecturerId = newId
                                Log.d("IMPORT_AUTH_DEBUG", "Created new lecturer: ${parsed.lecturerName}")
                            }
                        }
                        ImportResolution.SKIP -> {
                            lecturerId = (lecturerConflict?.existingItem as? LecturerEntity)?.lecturerId ?: ""
                        }
                        ImportResolution.DUPLICATE -> {
                            val newId = java.util.UUID.randomUUID().toString()
                            finalLecturers.add(LecturerEntity(
                                lecturerId = newId,
                                lecturerName = parsed.lecturerName,
                                email = email, // Will likely fail Room PK if same email, but user asked for it
                                department = parsed.department.ifBlank { "Unknown" },
                                passwordHash = authManager.hashPassword(parsed.lecturerPassword),
                                mustChangePassword = true
                            ))
                            lecturerId = newId
                        }
                        else -> {}
                    }

                    // Resolve Course
                    val courseConflict = _pendingConflicts.value.find { 
                        it.type == ConflictType.COURSE_CONFLICT && 
                        (it.newItem as Course).courseCode.equals(parsed.courseCode, ignoreCase = true) 
                    }
                    
                    val courseRes = courseConflict?.resolution ?: ImportResolution.OVERWRITE
                    
                    val resolutions = _missingFieldResolutions.value[parsed.hashCode()] ?: emptyMap()
                    val finalDay = resolutions["Course Day"] ?: parsed.day
                    val finalStartTime = resolutions["Start Time"] ?: parsed.startTime

                    when (courseRes) {
                        ImportResolution.OVERWRITE -> {
                            val existing = finalCourses.find { it.courseCode.equals(parsed.courseCode, ignoreCase = true) }
                            val updated = Course(
                                id = existing?.id ?: 0,
                                courseCode = parsed.courseCode,
                                courseName = parsed.courseName,
                                lecturerName = parsed.lecturerName,
                                lecturerID = lecturerId,
                                departmentIndex = mapDepartmentToIndex(parsed.department),
                                dayIndex = mapDayToIndex(finalDay),
                                timeSlotIndex = mapTimeToSlotIndex(finalStartTime),
                                courseClass = resolutions["Class"] ?: parsed.courseClass,
                                classroomType = resolutions["Room"] ?: parsed.classroomType,
                                duration = parsed.duration
                            )
                            if (existing != null) {
                                finalCourses[finalCourses.indexOf(existing)] = updated
                            } else {
                                finalCourses.add(updated)
                            }
                        }
                        ImportResolution.DUPLICATE -> {
                            finalCourses.add(Course(
                                courseCode = parsed.courseCode,
                                courseName = parsed.courseName,
                                lecturerName = parsed.lecturerName,
                                lecturerID = lecturerId,
                                departmentIndex = mapDepartmentToIndex(parsed.department),
                                dayIndex = mapDayToIndex(finalDay),
                                timeSlotIndex = mapTimeToSlotIndex(finalStartTime),
                                courseClass = resolutions["Class"] ?: parsed.courseClass,
                                classroomType = resolutions["Room"] ?: parsed.classroomType,
                                duration = parsed.duration
                            ))
                        }
                        ImportResolution.SKIP -> {}
                        else -> {}
                    }
                }

                // 4. Save to DB
                Log.d("FINAL_IMPORT_DEBUG", "Starting DB Write: ${finalCourses.size} courses, ${finalLecturers.size} lecturers")
                courseRepository.replaceAll(finalCourses)
                lecturerRepository.replaceAll(finalLecturers)

                // 5. Generate Schedules
                generateAutoSchedules(finalCourses)
                Log.d("FINAL_IMPORT_DEBUG", "DB Write and Schedule Generation Success. UI should refresh via Flow.")

                // 6. Auto-create Departments
                val distinctDepts = summary.parsedCourses.map { it.department }.filter { it.isNotBlank() }.distinct()
                val deptEntities = distinctDepts.map { DepartmentEntity(departmentName = it) }
                db.departmentDao().insertAll(deptEntities)

                _pendingConflicts.value = emptyList<ImportConflict>()
                _importSummary.value = null
                _uiState.value = UiState.Success(Unit)
                android.util.Log.d("IMPORT_PIPELINE_DEBUG", "IMPORT_COMPLETE - All data reflected in DB")
            } catch (e: Exception) {
                android.util.Log.e("CourseImportDebug", "IMPORT_FAILED: ${e.message}")
                handleError(e)
            }
        }
    }

    private suspend fun generateAutoSchedules(courses: List<Course>) {
        val schedules = mutableListOf<ScheduleEntity>()
        courses.forEach { course ->
            for (i in 0 until course.duration) {
                val slotIndex = course.timeSlotIndex + i
                if (slotIndex < 9) {
                    schedules.add(ScheduleEntity(
                        weekday = course.dayIndex,
                        startTime = ScheduleMatrixManager.timeSlotLabels[slotIndex].split("–")[0],
                        endTime = ScheduleMatrixManager.timeSlotLabels[slotIndex].split("–")[1],
                        timeSlotIndex = slotIndex,
                        courseId = course.id,
                        lecturerId = course.lecturerID,
                        departmentId = course.departmentIndex
                    ))
                }
            }
        }
        db.scheduleDao().replaceAll(schedules)
    }

    private fun mapDayToIndex(day: String?): Int {
        if (day == null || day.isBlank()) return -1
        val d = day.trim().lowercase()
        val index = when {
            d.contains("mon") -> 0
            d.contains("tue") -> 1
            d.contains("wed") -> 2
            d.contains("thu") -> 3
            d.contains("fri") -> 4
            else -> -1
        }
        Log.d("FINAL_IMPORT_DEBUG", "CourseDay mapping result: '$day' -> $index")
        return index
    }

    private fun mapTimeToSlotIndex(time: String?): Int {
        if (time == null) return 0
        val hour = time.filter { it.isDigit() }.take(2).toIntOrNull() ?: 8
        return when (hour) {
            in 0..8 -> 0
            9 -> 1
            10 -> 2
            11 -> 3
            13 -> 4
            14 -> 5
            15 -> 6
            16 -> 7
            17 -> 8
            else -> 0
        }
    }

    private fun mapDepartmentToIndex(dept: String?): Int {
        if (dept == null) return 0
        val d = dept.trim().lowercase()
        return when {
            d.contains("comp") -> 0
            d.contains("elect") -> 1
            d.contains("mech") -> 2
            d.contains("aero") -> 3
            d.contains("agri") -> 4
            else -> 0
        }
    }
    
    fun clearImportSummary() {
        _importSummary.value = null
        _pendingConflicts.value = emptyList<ImportConflict>()
        _missingFieldResolutions.value = emptyMap()
    }

    fun logout(navController: androidx.navigation.NavController) {
        Log.d("APP_FLOW_DEBUG", "Logout initiated - Clearing all state")
        authManager.signOut()
        _importSummary.value = null
        _pendingConflicts.value = emptyList<ImportConflict>()
        
        navController.navigate("sign_in") {
            popUpTo(0) { inclusive = true }
        }
        Log.d("APP_FLOW_DEBUG", "Navigation stack cleared, redirected to SignIn")
    }

    private fun syncMatrixWithDatabase(courses: List<Course>) {
        ScheduleMatrixManager.clearAll()
        courses.forEach { course ->
            ScheduleMatrixManager.setCourse(
                course.departmentIndex,
                course.dayIndex,
                course.timeSlotIndex,
                course
            )
        }
    }

    fun submitScheduleRequest(dayIndex: Int, slotIndex: Int, note: String, type: String) {
        val user = authManager.currentUserFlow.value ?: return
        viewModelScope.launch {
            try {
                val request = ScheduleRequestEntity(
                    lecturerId = user.id,
                    lecturerName = user.fullName,
                    weekday = dayIndex,
                    timeSlot = slotIndex,
                    note = note,
                    requestType = type,
                    status = "PENDING"
                )
                db.scheduleRequestDao().insert(request)
                Log.d("SAFE_CALENDAR_ENGINE", "request creation: Success - $type for day $dayIndex slot $slotIndex")
            } catch (e: Exception) {
                Log.e("SAFE_CALENDAR_ENGINE", "request creation: Failed - ${e.message}")
            }
        }
    }

    fun handleRequestAction(request: ScheduleRequestEntity, approve: Boolean) {
        viewModelScope.launch {
            try {
                val newStatus = if (approve) "APPROVED" else "REJECTED"
                val updated = request.copy(status = newStatus)
                db.scheduleRequestDao().updateRequest(updated)
                
                Log.d("SAFE_CALENDAR_ENGINE", "admin request action: $newStatus for request ${request.requestId}")
                
                if (approve) {
                    // In a real app, you'd update the schedule here.
                    // For now, we just update the status as requested.
                }
            } catch (e: Exception) {
                Log.e("SAFE_CALENDAR_ENGINE", "admin request action: Failed - ${e.message}")
            }
        }
    }

    private val _allRequests = MutableStateFlow<List<com.coursescheduling.domain.model.ScheduleRequestEntity>>(emptyList())
    val allRequests: StateFlow<List<com.coursescheduling.domain.model.ScheduleRequestEntity>> = _allRequests.asStateFlow()

    fun observeRequests() {
        val user = authManager.currentUserFlow.value ?: return
        viewModelScope.launch {
            if (user.role == com.coursescheduling.domain.model.Role.ADMIN) {
                db.scheduleRequestDao().getAllRequests().collect { _allRequests.value = it }
            } else {
                db.scheduleRequestDao().getRequestsByLecturer(user.id).collect { _allRequests.value = it }
            }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }
}
