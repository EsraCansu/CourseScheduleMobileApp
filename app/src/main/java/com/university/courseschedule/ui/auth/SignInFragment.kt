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
import com.university.courseschedule.databinding.FragmentSigninBinding

class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager.getInstance(requireContext())

        setupRoleDropdown()
        setupClickListeners()
    }

    private fun setupRoleDropdown() {
        val roles = arrayOf("Admin", "Lecturer")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roles
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerRole.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            performSignIn()
        }

        binding.tvSignUp.setOnClickListener {
            // Navigate to SignUp fragment
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }

    private fun performSignIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val role = binding.spinnerRole.selectedItem.toString()

        // Validation
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_field_required)
            return
        }
        binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_field_required)
            return
        }
        binding.tilPassword.error = null

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            return
        }

        // Attempt sign in
        val user = authManager.signIn(email, password, role)

        if (user != null) {
            // Store current user ID for session management
            authManager.setCurrentUserId(user.id)
            // Successful sign in - navigate to Home
            Toast.makeText(requireContext(), getString(R.string.welcome_message, user.fullName), Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
        } else {
            // Failed sign in
            Toast.makeText(requireContext(), getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
