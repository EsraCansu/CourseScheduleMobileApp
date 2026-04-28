package com.university.courseschedule.ui.data

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.ConflictManager
import com.university.courseschedule.data.FirestoreManager
import com.university.courseschedule.data.ImportConflict
import com.university.courseschedule.data.ScheduleMatrixManager
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer
import com.university.courseschedule.data.model.LecturerWithCourses
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.databinding.FragmentDataBinding
import com.university.courseschedule.ui.CourseViewModel
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * DataFragment manages the import and display of course schedule data.
 * 
 * Lecturer-Centric UI: The primary entity is the Lecturer, with their
 * assigned courses grouped underneath. Each lecturer appears only once.
 * 
 * UI State Machine (Room Database as single source of truth):
 * - EMPTY_STATE: Database empty + no file selected -> Show centered '+' button and hint text
 * - FILE_SELECTED_STATE: File picked but not synced -> Show filename and 'SYNC DATA TO SYSTEM' button
 * - DATA_LOADED_STATE: Database has records -> Show RecyclerView with lecturer cards + persistent Update FAB
 */
class DataFragment : Fragment() {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!

    // Shared with CalendarFragment - same Activity scope.
    private val viewModel: CourseViewModel by activityViewModels()

    private lateinit var lecturerAdapter: LecturerCardAdapter

    // Auth manager for creating users from imported data
    private lateinit var authManager: AuthManager
    
    // Firestore manager for cloud sync operations
    private lateinit var firestoreManager: FirestoreManager

    // Store selected file URI for processing later
    private var selectedFileUri: Uri? = null

    // Track unique lecturers to avoid duplicate user creation
    private val processedLecturers = mutableMapOf<String, Lecturer>()

    // System file picker - returns a persistable URI.
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Store URI for later processing
            selectedFileUri = uri
            showFileSelectedState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AuthManager
        authManager = AuthManager.getInstance(requireContext())
        
        // Initialize FirestoreManager
        firestoreManager = FirestoreManager.getInstance()

        setupRecyclerView()
        setupClickListeners()

        // Initialize matrix from database on fragment creation
        // This ensures navigation back from other fragments shows data immediately
        initializeFromDatabase()

        // Single source of truth: observe both courses and lecturers from Room Database
        // The UI state machine transitions based on database content and file selection status
        
        // Observe courses to determine if data exists in database
        viewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            if (courses.isNullOrEmpty()) {
                // Database is empty - check if file is selected
                if (selectedFileUri != null) {
                    showFileSelectedState()
                } else {
                    showEmptyState()
                }
            } else {
                // Database has courses - also sync matrix with database
                syncMatrix(courses)
            }
        }
        
        // Observe grouped lecturers with courses to populate RecyclerView
        viewModel.lecturersWithCourses.observe(viewLifecycleOwner) { grouped ->
            if (!grouped.isNullOrEmpty()) {
                lecturerAdapter.submitList(grouped)
                // Only show processed state if we also have courses
                viewModel.allCourses.value?.let { courses ->
                    if (courses.isNotEmpty()) {
                        showDataLoadedState()
                    }
                }
            }
        }
    }

    /**
     * Initialize matrix from database when fragment is created.
     * This ensures that navigating to other fragments and returning
     * does not result in a blank screen in CalendarFragment.
     */
    private fun initializeFromDatabase() {
        // Check if database already has data and load matrix accordingly
        viewModel.allCourses.value?.let { courses ->
            if (courses.isNotEmpty()) {
                syncMatrix(courses)
            }
        }
        
        // Also trigger matrix load from ViewModel
        viewModel.loadMatrixFromDatabase()
    }

    private fun setupRecyclerView() {
        lecturerAdapter = LecturerCardAdapter { lecturerWithCourses ->
            showLecturerDetailBottomSheet(lecturerWithCourses)
        }
        binding.rvImportedData.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lecturerAdapter
        }
    }

    private fun setupClickListeners() {
        // File selection button - only visible in empty state
        binding.fabImport.setOnClickListener {
            launchFilePicker()
        }

        // SYNC DATA TO SYSTEM button - visible only in the file-selected state
        binding.btnProcess.setOnClickListener {
            selectedFileUri?.let { uri -> processImportedFile(uri) }
        }

        // Persistent "Update Data" FAB - visible in DATA_LOADED_STATE
        // Allows Admin to re-import / replace existing dataset
        binding.fabUpdateData.setOnClickListener {
            launchFilePicker()
        }

        // "Push to Cloud" button - visible only for Admin when data is loaded
        binding.btnPushToCloud.setOnClickListener {
            pushDataToCloud()
        }
    }

    private fun launchFilePicker() {
        filePicker.launch(
            arrayOf(
                "text/plain",
                "text/csv",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel"
            )
        )
    }

    /**
     * EMPTY_STATE: Database empty and no file selected.
     * Shows only the centered '+' button and hint text.
     */
    private fun showEmptyState() {
        binding.fabImport.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.layoutFileSelected.visibility = View.GONE
        binding.btnProcess.visibility = View.GONE
        binding.rvImportedData.visibility = View.GONE
        binding.fabUpdateData.visibility = View.GONE
    }

    /**
     * FILE_SELECTED_STATE: File picked but not yet synced to database.
     * Displays the filename and the 'SYNC DATA TO SYSTEM' button.
     */
    private fun showFileSelectedState() {
        selectedFileUri?.let { uri ->
            val fileName = resolveDisplayName(uri)
            binding.tvSelectedFileName.text = fileName
        }
        
        binding.fabImport.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
        binding.layoutFileSelected.visibility = View.VISIBLE
        binding.btnProcess.visibility = View.VISIBLE
        binding.rvImportedData.visibility = View.GONE
        binding.fabUpdateData.visibility = View.GONE
    }

    /**
     * DATA_LOADED_STATE: Database contains records.
     * Shows the RecyclerView with lecturer cards and the persistent Update Data FAB.
     * Also shows "Push to Cloud" button for Admin users when Firebase is configured.
     */
    private fun showDataLoadedState() {
        binding.fabImport.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
        binding.layoutFileSelected.visibility = View.GONE
        binding.btnProcess.visibility = View.GONE
        binding.rvImportedData.visibility = View.VISIBLE
        binding.fabUpdateData.visibility = View.VISIBLE
        
        // Show "Push to Cloud" button for Admin users when Firebase is configured
        val currentUser = authManager.getCurrentUser()
        val isAdmin = currentUser?.role == Role.ADMIN
        val isFirebaseConfigured = FirestoreManager.isFirebaseConfigured()
        
        if (isAdmin && isFirebaseConfigured) {
            binding.btnPushToCloud.visibility = View.VISIBLE
            binding.tvCloudStatus.visibility = View.VISIBLE
            binding.tvCloudStatus.text = "Cloud sync available"
        } else {
            binding.btnPushToCloud.visibility = View.GONE
            binding.tvCloudStatus.visibility = View.GONE
        }
    }

    // -- Lecturer Detail BottomSheet --

    /**
     * Shows a BottomSheetDialog with the lecturer's detail info:
     * - Full name and email
     * - System password from the imported file
     * - List of all assigned courses
     */
    private fun showLecturerDetailBottomSheet(item: LecturerWithCourses) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_lecturer_detail, null)

        sheetView.findViewById<TextView>(R.id.tvDetailLecturerName).text = item.lecturer.lecturerName
        sheetView.findViewById<TextView>(R.id.tvDetailEmail).text = item.lecturer.email
        sheetView.findViewById<TextView>(R.id.tvDetailPassword).text =
            if (item.lecturer.password.isNotEmpty()) item.lecturer.password else "(not set)"

        // Setup courses list inside the BottomSheet
        val rvCourses = sheetView.findViewById<RecyclerView>(R.id.rvDetailCourses)
        rvCourses.layoutManager = LinearLayoutManager(requireContext())
        rvCourses.adapter = CourseDetailAdapter(item.courses)

        dialog.setContentView(sheetView)
        dialog.show()
    }

    /**
     * Parses the selected file and persists results to Room Database.
     * This is called ONLY when user clicks "SYNC DATA TO SYSTEM" button.
     * Performs a transaction: deleteAll() followed by insertAll(newCourses).
     */
    private fun processImportedFile(uri: Uri) {
        val mimeType = requireContext().contentResolver.getType(uri) ?: ""
        val parsedCourses = mutableListOf<Course>()
        val parsedLecturers = mutableListOf<Lecturer>()
        processedLecturers.clear()
        val fileName = uri.lastPathSegment ?: ""

        when {
            mimeType == "text/plain" || fileName.endsWith(".txt") -> {
                requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.useLines { lines ->
                        lines.forEach { line ->
                            parseTxtLine(line)?.let { parsedCourses.add(it) }
                        }
                    }
            }
            mimeType == "text/csv" || fileName.endsWith(".csv") -> {
                requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.useLines { lines ->
                        lines.forEach { line ->
                            parseCsvLine(line)?.let { parsedCourses.add(it) }
                        }
                    }
            }
            // XLSX parsing with Apache POI
            mimeType.contains("spreadsheet") || fileName.endsWith(".xlsx") || fileName.endsWith(".xls") -> {
                try {
                    parseExcelFile(uri, parsedCourses, parsedLecturers)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error parsing Excel file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }
        }

        if (parsedCourses.isNotEmpty() || parsedLecturers.isNotEmpty()) {
            // First check for conflicts with existing data
            checkAndShowConflictDialog(parsedCourses, parsedLecturers)
        } else {
            Toast.makeText(
                requireContext(),
                "No valid data found in file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Checks for conflicts with existing data and shows a dialog if conflicts exist.
     * Allows user to choose "Overwrite" or "Keep" existing data.
     */
    private fun checkAndShowConflictDialog(
        parsedCourses: List<Course>,
        parsedLecturers: List<Lecturer>
    ) {
        val existingCourses = viewModel.allCourses.value ?: emptyList()
        val existingLecturers = viewModel.allLecturers.value ?: emptyList()
        
        val conflicts = ConflictManager.getAllConflicts(
            existingCourses, parsedCourses,
            existingLecturers, parsedLecturers
        )
        
        if (conflicts.isEmpty()) {
            // No conflicts - proceed directly with import
            saveImportedData(parsedCourses, parsedLecturers)
        } else {
            // Show conflict dialog
            showConflictDialog(conflicts) { overwrite ->
                if (overwrite) {
                    // Proceed with import (overwrite mode)
                    saveImportedData(parsedCourses, parsedLecturers)
                } else {
                    // Keep existing data - filter out conflicting items
                    val filteredCourses = filterOutConflictingCourses(parsedCourses, existingCourses)
                    val filteredLecturers = filterOutConflictingLecturers(parsedLecturers, existingLecturers)
                    saveImportedData(filteredCourses, filteredLecturers)
                }
            }
        }
    }

    /**
     * Shows a dialog listing all conflicts and asks user to choose Overwrite or Keep.
     */
    private fun showConflictDialog(
        conflicts: List<ImportConflict>,
        onChoice: (overwrite: Boolean) -> Unit
    ) {
        val conflictDescriptions = conflicts.map { it.description }
        val message = buildString {
            append("Found ${conflicts.size} conflict(s):\n\n")
            conflictDescriptions.take(5).forEach { append("• $it\n") }
            if (conflicts.size > 5) {
                append("...and ${conflicts.size - 5} more")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Data Conflict Detected")
            .setMessage(message)
            .setPositiveButton("Overwrite") { _, _ ->
                onChoice(true)
            }
            .setNegativeButton("Keep Existing") { _, _ ->
                onChoice(false)
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // Clear selected file on cancel
                selectedFileUri = null
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Filters out courses that would conflict with existing data.
     */
    private fun filterOutConflictingCourses(
        newCourses: List<Course>,
        existingCourses: List<Course>
    ): List<Course> {
        val existingKeys = existingCourses.map { 
            "${it.departmentIndex}_${it.dayIndex}_${it.timeSlotIndex}" 
        }.toSet()
        
        return newCourses.filter { course ->
            val key = "${course.departmentIndex}_${course.dayIndex}_${course.timeSlotIndex}"
            key !in existingKeys
        }
    }

    /**
     * Filters out lecturers that would conflict with existing data.
     */
    private fun filterOutConflictingLecturers(
        newLecturers: List<Lecturer>,
        existingLecturers: List<Lecturer>
    ): List<Lecturer> {
        val existingNames = existingLecturers.map { 
            it.lecturerName.lowercase() 
        }.toSet()
        
        return newLecturers.filter { lecturer ->
            lecturer.lecturerName.lowercase() !in existingNames
        }
    }

    /**
     * Saves the imported data to the database.
     * This is called after conflict resolution.
     */
    private fun saveImportedData(
        parsedCourses: List<Course>,
        parsedLecturers: List<Lecturer>
    ) {
        // Create users for imported lecturers with their passwords
        // This ensures authentication alignment - lecturer names and passwords
        // from the file are accessible for Sign In system validation
        parsedLecturers.forEach { lecturer ->
            if (lecturer.password.isNotEmpty()) {
                authManager.createOrUpdateLecturerFromImport(
                    lecturerID = lecturer.lecturerID,
                    lecturerName = lecturer.lecturerName,
                    email = lecturer.email,
                    password = lecturer.password,
                    departmentIndex = lecturer.departmentIndex
                )
            }
        }

        // -- SAVE TO ROOM DATABASE: CRITICAL STEP --
        // Perform transaction: deleteAll() followed by insertAll()
        // Room Database is the single source of truth
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Batch fetch all existing lecturers for O(1) lookups
                val existingLecturers = viewModel.allLecturers.value ?: emptyList()
                val existingMap = existingLecturers.associateBy { it.lecturerName }

                val updatedLecturers = mutableListOf<Lecturer>()

                for (lecturer in parsedLecturers) {
                    val existingLecturer = existingMap[lecturer.lecturerName]
                    if (existingLecturer != null) {
                        // Update existing lecturer - keep same ID
                        updatedLecturers.add(
                            lecturer.copy(
                                id = existingLecturer.id,
                                lecturerID = existingLecturer.lecturerID
                            )
                        )
                    } else {
                        // New lecturer
                        updatedLecturers.add(lecturer)
                    }
                }

                // Save to Room - this triggers the LiveData observer
                // Uses atomic transaction: replaceAll does deleteAll + insertAll
                viewModel.replaceAll(parsedCourses, updatedLecturers)

                // Clear selected file after successful save
                selectedFileUri = null

                Toast.makeText(
                    requireContext(),
                    "Imported ${parsedCourses.size} courses and ${parsedLecturers.size} lecturers",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error saving data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // UI will be updated by the LiveData observer
    }

    /**
     * Parses an Excel (.xlsx) file
     */
    private fun parseExcelFile(
        uri: Uri,
        courses: MutableList<Course>,
        lecturers: MutableList<Lecturer>
    ) {
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            // Skip header row
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue

                val courseCode = getCellStringValue(row, 0)
                val courseName = getCellStringValue(row, 1)
                val lecturerName = getCellStringValue(row, 2)
                val password = getCellStringValue(row, 3)

                if (courseCode.isEmpty() && courseName.isEmpty()) continue

                val lecturerID = if (processedLecturers.containsKey(lecturerName)) {
                    processedLecturers[lecturerName]?.lecturerID ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }

                val email = generateEmailFromName(lecturerName)

                if (!processedLecturers.containsKey(lecturerName)) {
                    val lecturer = Lecturer(
                        lecturerID = lecturerID,
                        lecturerName = lecturerName,
                        courseCode = courseCode,
                        email = email,
                        password = password,
                        departmentIndex = 0
                    )
                    processedLecturers[lecturerName] = lecturer
                    lecturers.add(lecturer)
                }

                val course = Course(
                    courseCode = courseCode,
                    courseName = courseName,
                    lecturerName = lecturerName,
                    lecturerID = lecturerID,
                    password = password,
                    departmentIndex = 0,
                    dayIndex = 0,
                    timeSlotIndex = 0
                )
                courses.add(course)
            }

            workbook.close()
        }
    }

    private fun getCellStringValue(row: org.apache.poi.ss.usermodel.Row, columnIndex: Int): String {
        val cell = row.getCell(columnIndex) ?: return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
            CellType.FORMULA -> {
                when (cell.cellType) {
                    CellType.STRING -> cell.stringCellValue ?: ""
                    CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
                    else -> ""
                }
            }
            else -> ""
        }
    }

    private fun generateEmailFromName(name: String): String {
        val cleanName = name.trim().lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .replace(Regex("\\s+"), ".")
        return "$cleanName@university.edu"
    }

    private fun parseTxtLine(line: String): Course? {
        val parts = line.trim().split("|")
        if (parts.size < 7) return null
        return try {
            Course(
                courseCode     = parts[0].trim(),
                courseName     = parts[1].trim(),
                lecturerName   = parts[2].trim(),
                lecturerID     = parts[3].trim(),
                departmentIndex = parts[4].trim().toInt(),
                dayIndex       = parts[5].trim().toInt(),
                timeSlotIndex  = parts[6].trim().toInt()
            )
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun parseCsvLine(line: String): Course? {
        val parts = line.trim().split(",")
        if (parts.size < 7) return null
        return try {
            Course(
                courseCode     = parts[0].trim(),
                courseName     = parts[1].trim(),
                lecturerName   = parts[2].trim(),
                lecturerID     = parts[3].trim(),
                departmentIndex = parts[4].trim().toInt(),
                dayIndex       = parts[5].trim().toInt(),
                timeSlotIndex  = parts[6].trim().toInt()
            )
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Synchronizes the ScheduleMatrixManager with the Room Database.
     * This is called whenever the database is updated to ensure CalendarFragment
     * renders the correct schedule.
     */
    private fun syncMatrix(courses: List<Course>) {
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

    private fun resolveDisplayName(uri: Uri): String =
        requireContext().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: getString(R.string.data_unknown_file)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -- Lecturer Card Adapter (main list) --

    /**
     * Displays Lecturer Cards in the main RecyclerView.
     * Each card shows the lecturer's full name and course count.
     * Tapping a card triggers the detail BottomSheet.
     */
    private class LecturerCardAdapter(
        private val onItemClick: (LecturerWithCourses) -> Unit
    ) : RecyclerView.Adapter<LecturerCardAdapter.ViewHolder>() {

        private var items = listOf<LecturerWithCourses>()

        fun submitList(newList: List<LecturerWithCourses>) {
            items = newList
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivProfileImage: ShapeableImageView = view.findViewById(R.id.ivProfileImage)
            val tvLecturerName: TextView = view.findViewById(R.id.tvLecturerName)
            val tvCourseCount: TextView = view.findViewById(R.id.tvCourseCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lecturer_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvLecturerName.text = item.lecturer.lecturerName
            val count = item.courses.size
            holder.tvCourseCount.text = if (count == 1) "1 course" else "$count courses"
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }

    // -- Course Detail Adapter (inside BottomSheet) --

    /**
     * Displays course rows inside the Lecturer Detail BottomSheet.
     * Shows course code chip + course name for each assigned course.
     */
    private class CourseDetailAdapter(
        private val courses: List<Course>
    ) : RecyclerView.Adapter<CourseDetailAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCourseCode: TextView = view.findViewById(R.id.tvDetailCourseCode)
            val tvCourseName: TextView = view.findViewById(R.id.tvDetailCourseName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_detail_course, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val course = courses[position]
            holder.tvCourseCode.text = course.courseCode
            holder.tvCourseName.text = course.courseName
        }

        override fun getItemCount(): Int = courses.size
    }
    
    // -- Cloud Sync Methods --
    
    /**
     * Pushes all local data (courses and lecturers) to Firebase Firestore.
     * This is the "Deduplicate & Push to Cloud" flow for Admin.
     * Uses FirestoreManager for cloud operations.
     */
    private fun pushDataToCloud() {
        // Get current data from ViewModel
        val courses = viewModel.allCourses.value
        val lecturers = viewModel.allLecturers.value
        
        if (courses.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "No data to push to cloud",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Show progress
        binding.btnPushToCloud.isEnabled = false
        binding.tvCloudStatus.text = "Pushing to cloud..."
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Push to cloud - this implements the "Excel Append Logic" (Deduplicate & Push)
                // The FirestoreManager uses SetOptions.merge() which handles deduplication
                val success = firestoreManager.pushAllToCloud(
                    courses = courses,
                    lecturers = lecturers ?: emptyList()
                )
                
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Successfully pushed ${courses.size} courses to cloud",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvCloudStatus.text = "Synced to cloud"
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to push to cloud. Check Firebase configuration.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tvCloudStatus.text = "Cloud sync failed"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error pushing to cloud: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.tvCloudStatus.text = "Cloud sync error"
            } finally {
                binding.btnPushToCloud.isEnabled = true
            }
        }
    }
}
