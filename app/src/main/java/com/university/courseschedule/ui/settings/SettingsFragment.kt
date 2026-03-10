package com.university.courseschedule.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.university.courseschedule.R
import com.university.courseschedule.data.model.Department
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.databinding.FragmentSettingsBinding
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_DEPARTMENT
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_IS_REGISTERED
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_NAME
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_ROLE
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_SURNAME
import com.university.courseschedule.ui.home.HomeFragment.Companion.PREFS_NAME
import java.util.UUID

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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
        setupDropdowns()
        loadSavedProfile()
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

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun loadSavedProfile() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        binding.etName.setText(prefs.getString(KEY_NAME, ""))
        binding.etSurname.setText(prefs.getString(KEY_SURNAME, ""))

        val savedDept = prefs.getString(KEY_DEPARTMENT, null)
        if (savedDept != null) {
            val index = Department.values().indexOfFirst { it.displayName == savedDept }
            if (index >= 0) binding.spinnerDepartment.setSelection(index)
        }

        val savedRole = prefs.getString(KEY_ROLE, null)
        if (savedRole != null) {
            val index = Role.values().indexOfFirst {
                it.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() } == savedRole
            }
            if (index >= 0) binding.spinnerRole.setSelection(index)
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val surname = binding.etSurname.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_name_required, Toast.LENGTH_SHORT).show()
            return
        }

        val department = binding.spinnerDepartment.selectedItem.toString()
        val role = binding.spinnerRole.selectedItem.toString()

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = prefs.getString("user_id", null) ?: UUID.randomUUID().toString()

        prefs.edit()
            .putString("user_id", existingId)
            .putString(KEY_NAME, name)
            .putString(KEY_SURNAME, surname)
            .putString(KEY_DEPARTMENT, department)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()

        Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()

        // Navigate back to Home so the welcome card refreshes and the
        // registration guard re-evaluates with the newly saved profile.
        findNavController().navigate(R.id.action_settingsFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
