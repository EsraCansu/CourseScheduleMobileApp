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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.ScheduleMatrixManager
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer
import com.university.courseschedule.databinding.FragmentDataBinding
import com.university.courseschedule.ui.CourseViewModel
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.util.UUID

class DataFragment : Fragment() {

    private var _binding: FragmentDataBinding? = null
    private val binding get() = _binding!!

    // Shared with CalendarFragment — same Activity scope.
    private val viewModel: CourseViewModel by activityViewModels()

    private val displayItems = mutableListOf<Course>()
    private lateinit var listAdapter: CourseListAdapter

    // Auth manager for creating users from imported data
    private lateinit var authManager: AuthManager

    // Track unique lecturers to avoid duplicate user creation
    private val processedLecturers = mutableMapOf<String, Lecturer>()

    // System file picker — returns a persistable URI.
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            processImportedFile(uri)
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

        setupRecyclerView()

        // ── Reload from DB on every visit so navigation away never clears the list ──
        viewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            if (courses.isNullOrEmpty()) {
                showEmptyState()
            } else {
                displayItems.clear()
                displayItems.addAll(courses)
                listAdapter.notifyDataSetChanged()
                showImportedList()
            }
        }

        binding.fabImport.setOnClickListener {
            filePicker.launch(
                arrayOf(
                    "text/plain",
                    "text/csv",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                )
            )
        }
    }

    private fun setupRecyclerView() {
        listAdapter = CourseListAdapter(displayItems)
        binding.rvImportedData.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }
    }

    /**
     * Parses the selected file and persists results to Room.
     *
     * TXT format expected (one course per line, pipe-delimited):
     *   courseCode|courseName|lecturerName|lecturerID|deptIndex|dayIndex|slotIndex
     *
     * CSV format (comma-separated):
     *   courseCode,courseName,lecturerName,lecturerID,deptIndex,dayIndex,slotIndex
     *
     * XLSX format (Excel with headers):
     *   Course Code, Course Name, Lecturer, Passwords
     *
     * After a successful import the ViewModel writes to Room; the LiveData
     * observer above then updates the RecyclerView automatically.
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
            // XLSX parsing with Apache POI - supports Passwords column
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

        if (parsedCourses.isNotEmpty()) {
            // Create users for imported lecturers with their passwords
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

            // Persist to Room and sync the in-memory matrix.
            // Use the updated method that handles both courses and lecturers
            viewModel.replaceAll(parsedCourses, parsedLecturers)
            syncMatrix(parsedCourses)

            Toast.makeText(
                requireContext(),
                "Imported ${parsedCourses.size} courses and ${parsedLecturers.size} lecturers",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Parses an Excel (.xlsx) file with the following columns:
     * - Course Code
     * - Course Name
     * - Lecturer
     * - Passwords (optional)
     *
     * The first row is treated as headers and skipped.
     */
    private fun parseExcelFile(
        uri: Uri,
        courses: MutableList<Course>,
        lecturers: MutableList<Lecturer>
    ) {
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0) // Get first sheet

            // Skip header row, start from row 1
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue

                // Read cells - handle both numeric and string cell types
                val courseCode = getCellStringValue(row, 0)
                val courseName = getCellStringValue(row, 1)
                val lecturerName = getCellStringValue(row, 2)
                val password = getCellStringValue(row, 3)

                // Skip empty rows
                if (courseCode.isEmpty() && courseName.isEmpty()) continue

                // Generate lecturer ID if not already processed
                val lecturerID = if (processedLecturers.containsKey(lecturerName)) {
                    processedLecturers[lecturerName]?.lecturerID ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }

                // Generate email from lecturer name if not provided
                val email = generateEmailFromName(lecturerName)

                // Store lecturer with password (if provided)
                if (!processedLecturers.containsKey(lecturerName)) {
                    val lecturer = Lecturer(
                        lecturerID = lecturerID,
                        lecturerName = lecturerName,
                        email = email,
                        password = password,
                        departmentIndex = 0 // Default department, can be updated
                    )
                    processedLecturers[lecturerName] = lecturer
                    lecturers.add(lecturer)
                }

                // Create course with lecturer password stored for reference
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

    /**
     * Gets the string value from a cell, handling both string and numeric types.
     */
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

    /**
     * Generates an email address from the lecturer's name.
     * Example: "John Smith" -> "john.smith@university.edu"
     */
    private fun generateEmailFromName(name: String): String {
        val cleanName = name.trim().lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .replace(Regex("\\s+"), ".")
        return "$cleanName@university.edu"
    }

    /**
     * Parses a single pipe-delimited TXT line.
     * Returns null and silently skips malformed lines.
     * Format: courseCode|courseName|lecturerName|lecturerID|deptIndex|dayIndex|slotIndex
     */
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

    /**
     * Parses a single comma-delimited CSV line.
     * Returns null and silently skips malformed lines.
     * Format: courseCode,courseName,lecturerName,lecturerID,deptIndex,dayIndex,slotIndex
     */
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

    /** Writes parsed courses into the shared in-memory ScheduleMatrixManager. */
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

    private fun showEmptyState() {
        binding.fabImport.visibility      = View.VISIBLE
        binding.tvEmptyState.visibility   = View.VISIBLE
        binding.rvImportedData.visibility = View.GONE
    }

    /** Hides the FAB + empty-state; reveals the list. */
    private fun showImportedList() {
        binding.fabImport.visibility      = View.GONE
        binding.tvEmptyState.visibility   = View.GONE
        binding.rvImportedData.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private inner class CourseListAdapter(
        private val items: List<Course>
    ) : RecyclerView.Adapter<CourseListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvItemName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_imported_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val course = items[position]
            holder.tvName.text = "${course.courseCode} — ${course.courseName} (${course.lecturerName})"
        }

        override fun getItemCount(): Int = items.size
    }
}
