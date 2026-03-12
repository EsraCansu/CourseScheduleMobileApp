package com.university.courseschedule.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.model.Department
import com.university.courseschedule.databinding.FragmentSignupBinding

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager.getInstance(requireContext())

        setupDropdowns()
        setupClickListeners()
    }

    private fun setupDropdowns() {
        // Role dropdown
        val roles = arrayOf("Admin", "Lecturer")
        val roleAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roles
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerRole.adapter = roleAdapter

        // Department dropdown
        val deptAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Department.values().map { it.displayName }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerDepartment.adapter = deptAdapter
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            performSignUp()
        }

        binding.tvSignIn.setOnClickListener {
            // Navigate back to SignIn
            findNavController().popBackStack()
        }
    }

    private fun performSignUp() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val role = binding.spinnerRole.selectedItem.toString()
        val department = binding.spinnerDepartment.selectedItem.toString()

        // Validation
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.tilUsername.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_field_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_field_required)
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (!isValid) return

        // Attempt sign up
        val success = authManager.signUp(username, email, password, role, department)

        if (success) {
            // Auto-login after successful registration for better UX
            val user = authManager.signIn(email, password, role)
            if (user != null) {
                authManager.setCurrentUserId(user.id)
                Toast.makeText(requireContext(), getString(R.string.welcome_message, user.fullName), Toast.LENGTH_SHORT).show()
                // Navigate to Home
                findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Registration successful! Please sign in.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.error_email_exists), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
