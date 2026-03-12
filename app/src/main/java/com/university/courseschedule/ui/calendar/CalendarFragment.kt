package com.university.courseschedule.ui.calendar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.university.courseschedule.data.ScheduleMatrixManager
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Department
import com.university.courseschedule.databinding.FragmentCalendarBinding
import com.university.courseschedule.ui.CourseViewModel
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_DEPARTMENT
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_ROLE
import com.university.courseschedule.ui.home.HomeFragment.Companion.PREFS_NAME

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CourseViewModel by activityViewModels()

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

        // Build the cell look-up table from the layout IDs
        cells = arrayOf(
            arrayOf(binding.cell00, binding.cell01),
            arrayOf(binding.cell10, binding.cell11),
            arrayOf(binding.cell20, binding.cell21),
            arrayOf(binding.cell30, binding.cell31),
            arrayOf(binding.cell40, binding.cell41)
        )

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val role   = prefs.getString(KEY_ROLE, "").orEmpty()
        val userId = prefs.getString("user_id", "").orEmpty()

        if (role.equals("Admin", ignoreCase = true)) {
            setupAdminView(prefs.getString(KEY_DEPARTMENT, null))
        } else {
            setupLecturerView(userId)
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

