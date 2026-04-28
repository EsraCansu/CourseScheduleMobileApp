package com.university.courseschedule.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.LecturerAvailabilityManager
import com.university.courseschedule.data.ScheduleMatrixManager
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Department
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.databinding.FragmentCalendarBinding
import com.university.courseschedule.ui.CourseViewModel

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CourseViewModel by activityViewModels()
    private lateinit var authManager: AuthManager
    private lateinit var availabilityManager: LecturerAvailabilityManager

    // Track current mode: false = view courses, true = set availability
    private var isAvailabilityMode = false

    // cells[day 0-4][slot 0-1] — wired up in onViewCreated
    private lateinit var cells: Array<Array<TextView>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager.getInstance(requireContext())
        availabilityManager = LecturerAvailabilityManager.getInstance(requireContext())

        // Build the cell look-up table from the layout IDs
        cells = arrayOf(
            arrayOf(binding.cell00, binding.cell01),
            arrayOf(binding.cell10, binding.cell11),
            arrayOf(binding.cell20, binding.cell21),
            arrayOf(binding.cell30, binding.cell31),
            arrayOf(binding.cell40, binding.cell41)
        )

        // Setup click listeners for cells (for lecturer availability toggle)
        setupCellClickListeners()

        // Use AuthManager (single source of truth) instead of raw SharedPreferences
        val user = authManager.getCurrentUser()

        if (user?.role == Role.ADMIN) {
            setupAdminView(user.department.displayName)
        } else {
            setupLecturerView(user?.id ?: "")
        }

        // Show mode toggle only for lecturers
        if (user?.role == Role.LECTURER) {
            binding.layoutModeToggle.visibility = View.VISIBLE
        } else {
            binding.layoutModeToggle.visibility = View.GONE
        }
    }

    /**
     * Sets up click listeners for grid cells to toggle availability.
     * Only active for lecturers in availability mode.
     */
    private fun setupCellClickListeners() {
        for (day in 0 until ScheduleMatrixManager.DAY_COUNT) {
            for (slot in 0 until ScheduleMatrixManager.TIME_SLOT_COUNT) {
                cells[day][slot].setOnClickListener {
                    val user = authManager.getCurrentUser()
                    if (user?.role == Role.LECTURER && isAvailabilityMode) {
                        toggleAvailability(user.id, day, slot)
                    }
                }
            }
        }
    }

    /**
     * Toggles availability for a specific slot and updates the UI.
     */
    private fun toggleAvailability(lecturerId: String, day: Int, slot: Int) {
        val currentAvailability = availabilityManager.getAvailability(lecturerId, day, slot)
        val newAvailability = !currentAvailability
        availabilityManager.setAvailability(lecturerId, day, slot, newAvailability)
        
        // Update the cell appearance
        updateCellAppearance(lecturerId, day, slot, newAvailability)
        
        val status = if (newAvailability) "Available" else "Busy"
        Toast.makeText(
            requireContext(),
            "${LecturerAvailabilityManager.dayLabels[day]} ${LecturerAvailabilityManager.timeSlotLabels[slot]}: $status",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Updates the cell appearance based on availability status.
     */
    private fun updateCellAppearance(lecturerId: String, day: Int, slot: Int, isAvailable: Boolean) {
        val cell = cells[day][slot]
        if (isAvailable) {
            cell.setBackgroundColor(resources.getColor(R.color.teal_200, null))
            cell.text = "✓ Available"
        } else {
            cell.setBackgroundColor(resources.getColor(R.color.red_200, null))
            cell.text = "✗ Busy"
        }
    }

    /**
     * Refreshes the grid to show availability status for lecturer.
     */
    private fun refreshAvailabilityGrid(lecturerId: String) {
        val availability = availabilityManager.getAllAvailability(lecturerId)
        for (day in 0 until ScheduleMatrixManager.DAY_COUNT) {
            for (slot in 0 until ScheduleMatrixManager.TIME_SLOT_COUNT) {
                updateCellAppearance(lecturerId, day, slot, availability[day][slot])
            }
        }
    }

    // ── Admin view ────────────────────────────────────────────────────────────

    private fun setupAdminView(savedDeptName: String?) {
        binding.spinnerDepartment.visibility = View.VISIBLE

        val deptNames = Department.values().map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            deptNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerDepartment.adapter = adapter

        // Restore the department the user registered with as the default selection
        if (savedDeptName != null) {
            val idx = Department.values().indexOfFirst { it.displayName == savedDeptName }
            if (idx >= 0) binding.spinnerDepartment.setSelection(idx)
        }

        binding.spinnerDepartment.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, v: View?, position: Int, id: Long
                ) {
                    observeByDepartment(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun observeByDepartment(deptIndex: Int) {
        viewModel.getCoursesByDepartment(deptIndex).observe(viewLifecycleOwner) { courses ->
            ScheduleMatrixManager.clearDepartmentSchedule(deptIndex)
            courses.forEach {
                ScheduleMatrixManager.setCourse(it.departmentIndex, it.dayIndex, it.timeSlotIndex, it)
            }
            refreshGridFromMatrix(deptIndex)
        }
    }

    private fun refreshGridFromMatrix(deptIndex: Int) {
        val schedule = ScheduleMatrixManager.getDepartmentSchedule(deptIndex)
        for (day in 0 until ScheduleMatrixManager.DAY_COUNT) {
            for (slot in 0 until ScheduleMatrixManager.TIME_SLOT_COUNT) {
                val course = schedule[day][slot]
                cells[day][slot].text = course?.let { "${it.courseCode}\n${it.courseName}" } ?: ""
            }
        }
    }

    // ── Lecturer view ─────────────────────────────────────────────────────────

    private fun setupLecturerView(userId: String) {
        binding.spinnerDepartment.visibility = View.GONE

        // Setup toggle listener
        binding.toggleAvailabilityMode.setOnCheckedChangeListener { _, isChecked ->
            isAvailabilityMode = isChecked
            if (isChecked) {
                // Show availability grid
                refreshAvailabilityGrid(userId)
            } else {
                // Show courses
                viewModel.getCoursesByLecturer(userId).observe(viewLifecycleOwner) { courses ->
                    refreshLecturerGrid(courses)
                }
            }
        }

        viewModel.getCoursesByLecturer(userId).observe(viewLifecycleOwner) { courses ->
            refreshLecturerGrid(courses)
        }
    }

    /** Paints the lecturer's personal 5×2 grid without touching the shared matrix. */
    private fun refreshLecturerGrid(courses: List<Course>) {
        // Clear all cells first
        for (day in 0 until ScheduleMatrixManager.DAY_COUNT)
            for (slot in 0 until ScheduleMatrixManager.TIME_SLOT_COUNT)
                cells[day][slot].text = ""

        for (course in courses) {
            val day  = course.dayIndex
            val slot = course.timeSlotIndex
            if (day in 0..4 && slot in 0..1) {
                cells[day][slot].text = "${course.courseCode}\n${course.courseName}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
