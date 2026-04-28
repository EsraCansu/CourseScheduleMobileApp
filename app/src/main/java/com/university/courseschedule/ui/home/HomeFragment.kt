package com.university.courseschedule.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.FirebaseAuthManager
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.data.model.User
import com.university.courseschedule.databinding.FragmentHomeBinding
import com.university.courseschedule.ui.CourseViewModel
import com.university.courseschedule.ui.auth.SignInFragment
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager
    private lateinit var firebaseAuth: FirebaseAuthManager
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

        authManager  = AuthManager.getInstance(requireContext())
        firebaseAuth = FirebaseAuthManager.getInstance()

        setupClickListeners()
        setupRecyclerView()
        observeCourses()
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate auth state every time the screen becomes visible.
        // This handles: post-login refresh, post-logout refresh, back-navigation.
        refreshAuthState()
    }

    // ──────────────────────────────────────────────────────────────
    // Auth state
    // ──────────────────────────────────────────────────────────────

    /**
     * Determines auth state in priority order:
     *   1. Firebase Auth session (signed-in via Firebase)
     *   2. Local SharedPrefs session (offline / seed admin)
     *   3. Guest view
     */
    private fun refreshAuthState() {
        val firebaseUser = firebaseAuth.getFirebaseUser()

        if (firebaseUser != null) {
            // Firebase session active — fetch Firestore profile async
            lifecycleScope.launch {
                val profile = firebaseAuth.getUserProfile(firebaseUser.uid)
                if (profile != null) {
                    // Keep local session in sync
                    authManager.setCurrentUserId(firebaseUser.uid)
                    showLoggedInView(profile)
                } else {
                    // Firebase auth but no Firestore doc → fall back to local
                    fallbackLocalAuth()
                }
            }
        } else {
            fallbackLocalAuth()
        }
    }

    private fun fallbackLocalAuth() {
        if (authManager.isLoggedIn()) {
            val user = authManager.getCurrentUser()
            if (user != null) showLoggedInView(user) else showGuestView()
        } else {
            showGuestView()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // UI state switching
    // ──────────────────────────────────────────────────────────────

    private fun showGuestView() {
        binding.layoutGuest.visibility = View.VISIBLE
        binding.layoutUser.visibility  = View.GONE
    }

    private fun showLoggedInView(user: User) {
        binding.layoutGuest.visibility = View.GONE
        binding.layoutUser.visibility  = View.VISIBLE

        // Welcome text
        binding.tvWelcome.text = when {
            user.fullName.isNotEmpty() -> getString(R.string.welcome_message, user.fullName)
            else                       -> getString(R.string.welcome_guest)
        }
        binding.tvDepartment.text = user.department.displayName
        binding.tvRole.text       = user.role.name

        // First-login warning — Lecturer only, Admin exempt (spec §3.1)
        binding.cardFirstLoginWarning.visibility =
            if (user.role == Role.LECTURER && user.isFirstLogin) View.VISIBLE else View.GONE

        // Role-specific content
        if (user.role == Role.ADMIN) showAdminDashboard() else showLecturerView()
    }

    private fun showAdminDashboard() {
        binding.cardAdminDashboard.visibility   = View.VISIBLE
        binding.layoutLecturerCourses.visibility = View.GONE
    }

    private fun showLecturerView() {
        binding.cardAdminDashboard.visibility   = View.GONE
        binding.layoutLecturerCourses.visibility = View.VISIBLE
    }

    // ──────────────────────────────────────────────────────────────
    // Click listeners
    // ──────────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        // Sign In → opens BottomSheetDialogFragment
        binding.btnSignIn.setOnClickListener {
            SignInFragment().show(parentFragmentManager, SignInFragment.TAG)
        }

        // Logout — signs out from both Firebase and local SharedPrefs
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    /**
     * Signs out from Firebase Auth and local SharedPrefs session, then
     * refreshes the UI back to Guest view.
     */
    private fun performLogout() {
        firebaseAuth.signOut()      // Firebase session
        authManager.logout()        // Local SharedPrefs session
        showGuestView()
        // Reset welcome text to default
        binding.tvWelcome.text = getString(R.string.welcome_guest)
    }

    // ──────────────────────────────────────────────────────────────
    // RecyclerView + ViewModel
    // ──────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        coursesAdapter = LecturerCoursesAdapter(emptyList())
        binding.rvLecturerCourses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = coursesAdapter
        }
    }

    private fun observeCourses() {
        viewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            // Admin stats
            val courseCount     = courses?.size ?: 0
            val uniqueLecturers = courses?.map { it.lecturerID }?.distinct()?.size ?: 0
            binding.tvTotalCourses.text   = getString(R.string.total_courses, courseCount)
            binding.tvTotalLecturers.text = getString(R.string.total_lecturers, uniqueLecturers)

            // Lecturer courses list
            updateLecturerCourses(courses)
        }
    }

    private fun updateLecturerCourses(courses: List<Course>?) {
        if (!authManager.isLoggedIn()) return
        val user = authManager.getCurrentUser() ?: return
        if (user.role != Role.LECTURER) return

        val lecturerCourses = courses?.filter { it.lecturerID == user.id } ?: emptyList()
        if (lecturerCourses.isEmpty()) {
            binding.tvNoCourses.visibility      = View.VISIBLE
            binding.rvLecturerCourses.visibility = View.GONE
        } else {
            binding.tvNoCourses.visibility      = View.GONE
            binding.rvLecturerCourses.visibility = View.VISIBLE
            coursesAdapter.updateData(lecturerCourses)
        }
    }

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

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

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
        const val PREFS_NAME      = "user_prefs"
        const val KEY_IS_REGISTERED = "is_registered"
        const val KEY_NAME        = "name"
        const val KEY_SURNAME     = "surname"
        const val KEY_DEPARTMENT  = "department"
        const val KEY_ROLE        = "role"
    }
}
