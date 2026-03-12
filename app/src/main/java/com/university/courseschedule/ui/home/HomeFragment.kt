package com.university.courseschedule.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.databinding.FragmentHomeBinding
import com.university.courseschedule.ui.CourseViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager
    private val viewModel: CourseViewModel by activityViewModels()
    private lateinit var coursesAdapter: LecturerCoursesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager.getInstance(requireContext())

        setupClickListeners()
        setupRecyclerView()
        observeCourses()
    }

    override fun onResume() {
        super.onResume()
        checkAuthState()
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_signInFragment)
        }
    }

    private fun setupRecyclerView() {
        coursesAdapter = LecturerCoursesAdapter(emptyList())
        binding.rvLecturerCourses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coursesAdapter
        }
    }

    private fun observeCourses() {
        viewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            updateUI(courses)
            // Update admin dashboard stats
            val courseCount = courses?.size ?: 0
            binding.tvTotalCourses.text = getString(R.string.total_courses, courseCount)
            val uniqueLecturers = courses?.map { it.lecturerID }?.distinct()?.size ?: 0
            binding.tvTotalLecturers.text = getString(R.string.total_lecturers, uniqueLecturers)
        }
    }

    private fun checkAuthState() {
        if (authManager.isLoggedIn()) {
            showLoggedInView()
            loadUserProfile()
        } else {
            showGuestView()
        }
    }

    private fun showGuestView() {
        binding.layoutGuest.visibility = View.VISIBLE
        binding.layoutUser.visibility = View.GONE
    }

    private fun showLoggedInView() {
        binding.layoutGuest.visibility = View.GONE
        binding.layoutUser.visibility = View.VISIBLE

        val user = authManager.getCurrentUser()
        if (user?.role == Role.ADMIN) {
            showAdminDashboard()
        } else {
            showLecturerView()
        }
    }

    private fun showAdminDashboard() {
        binding.cardAdminDashboard.visibility = View.VISIBLE
        binding.layoutLecturerCourses.visibility = View.GONE
    }

    private fun showLecturerView() {
        binding.cardAdminDashboard.visibility = View.GONE
        binding.layoutLecturerCourses.visibility = View.VISIBLE
    }

    private fun loadUserProfile() {
        val user = authManager.getCurrentUser()
        
        binding.tvWelcome.text = when {
            user?.fullName?.isNotEmpty() == true ->
                getString(R.string.welcome_message, user.fullName)
            else -> getString(R.string.welcome_guest)
        }
        binding.tvDepartment.text = user?.department?.displayName ?: ""
        binding.tvRole.text = user?.role?.name ?: ""
    }

    private fun updateUI(courses: List<Course>?) {
        if (!authManager.isLoggedIn()) return

        val user = authManager.getCurrentUser()
        if (user?.role == Role.LECTURER) {
            // Filter courses for this lecturer using the user's ID
            val userId = user.id
            val lecturerCourses = courses?.filter { it.lecturerID == userId } ?: emptyList()

            if (lecturerCourses.isEmpty()) {
                binding.tvNoCourses.visibility = View.VISIBLE
                binding.rvLecturerCourses.visibility = View.GONE
            } else {
                binding.tvNoCourses.visibility = View.GONE
                binding.rvLecturerCourses.visibility = View.VISIBLE
                coursesAdapter.updateData(lecturerCourses)
            }
        }
    }

    private val prefs: SharedPreferences
        get() = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Adapter for Lecturer Courses ─────────────────────────────────────────────

    inner class LecturerCoursesAdapter(
        private var courses: List<Course>
    ) : RecyclerView.Adapter<LecturerCoursesAdapter.ViewHolder>() {

        fun updateData(newCourses: List<Course>) {
            courses = newCourses
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCourseInfo: View = view.findViewById(R.id.tvCourseInfo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_lecturer_course, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val course = courses[position]
            val tvCourseInfo = holder.itemView.findViewById<android.widget.TextView>(R.id.tvCourseInfo)
            tvCourseInfo.text = "${course.courseCode} - ${course.courseName}"
        }

        override fun getItemCount(): Int = courses.size
    }

    companion object {
        const val PREFS_NAME = "user_prefs"
        const val KEY_IS_REGISTERED = "is_registered"
        const val KEY_NAME = "name"
        const val KEY_SURNAME = "surname"
        const val KEY_DEPARTMENT = "department"
        const val KEY_ROLE = "role"
    }
}
