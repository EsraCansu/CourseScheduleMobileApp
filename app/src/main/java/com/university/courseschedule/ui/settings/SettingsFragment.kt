package com.university.courseschedule.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.FirebaseAuthManager
import com.university.courseschedule.data.model.Department
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager
    private lateinit var firebaseAuth: FirebaseAuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager  = AuthManager.getInstance(requireContext())
        firebaseAuth = FirebaseAuthManager.getInstance()

        setupDropdowns()
        loadSavedProfile()
        setupTextWatchers()
        setupDarkModeToggle()

        binding.btnSave.setOnClickListener { saveProfile() }
    }

    // ── Dropdown setup ────────────────────────────────────────────────────────

    private fun setupDropdowns() {
        val deptAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Department.values().map { it.displayName }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerDepartment.adapter = deptAdapter

        val roleAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Role.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerRole.adapter = roleAdapter
    }

    // ── Profile persistence (using AuthManager) ──────────────────────────────

    private fun loadSavedProfile() {
        val user = authManager.getCurrentUser() ?: return

        binding.etName.setText(user.name)
        binding.etSurname.setText(user.surname)

        val deptIndex = Department.values().indexOfFirst { it == user.department }
        if (deptIndex >= 0) binding.spinnerDepartment.setSelection(deptIndex)

        val roleIndex = Role.values().indexOfFirst { it == user.role }
        if (roleIndex >= 0) binding.spinnerRole.setSelection(roleIndex)
    }

    // ── TextWatcher for form validation (spec §3.4) ──────────────────────────

    /**
     * The Save button is disabled (isEnabled = false) if any required
     * fields are empty or the new password is < 6 characters.
     */
    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateForm() }
        }

        binding.etName.addTextChangedListener(watcher)
        binding.etSurname.addTextChangedListener(watcher)
        binding.etCurrentPassword.addTextChangedListener(watcher)
        binding.etNewPassword.addTextChangedListener(watcher)

        // Run initial validation
        validateForm()
    }

    private fun validateForm() {
        val name = binding.etName.text.toString().trim()
        val surname = binding.etSurname.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString()

        // Required fields must be non-empty
        val nameValid = name.isNotEmpty()
        val surnameValid = surname.isNotEmpty()

        // New password is optional, but if entered, must be >= 6 chars
        val passwordValid = newPassword.isEmpty() || newPassword.length >= 6

        binding.btnSave.isEnabled = nameValid && surnameValid && passwordValid

        // Show password length error hint
        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            binding.tilNewPassword.error = getString(R.string.error_password_too_short)
        } else {
            binding.tilNewPassword.error = null
        }
    }

    // ── Dark Mode ────────────────────────────────────────────────────────────

    private fun setupDarkModeToggle() {
        val prefs = requireContext().getSharedPreferences(PREFS_APPEARANCE, android.content.Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        binding.switchDarkMode.isChecked = isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    // ── Save profile ─────────────────────────────────────────────────────────

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val surname = binding.etSurname.text.toString().trim()
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()

        if (name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_name_required, Toast.LENGTH_SHORT).show()
            return
        }

        val department = binding.spinnerDepartment.selectedItem.toString()

        // Update profile via AuthManager (single source of truth)
        authManager.updateProfile(name, surname, department)

        // Handle password change if both fields are filled
        if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
            if (newPassword.length < 6) {
                binding.tilNewPassword.error = getString(R.string.error_password_too_short)
                return
            }

            // Change in local SharedPrefs (verifies current password)
            val changed = authManager.changePassword(currentPassword, newPassword)
            if (changed) {
                // Also change in Firebase Auth (async, best-effort)
                lifecycleScope.launch {
                    firebaseAuth.changePassword(newPassword)
                }
                Toast.makeText(requireContext(), R.string.password_changed, Toast.LENGTH_SHORT).show()
                binding.etCurrentPassword.text?.clear()
                binding.etNewPassword.text?.clear()
            } else {
                binding.tilCurrentPassword.error = getString(R.string.error_current_password_wrong)
                return
            }
        }

        Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()

        // Navigate back to Home so the welcome card refreshes
        findNavController().navigate(R.id.action_settingsFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PREFS_APPEARANCE = "appearance_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
