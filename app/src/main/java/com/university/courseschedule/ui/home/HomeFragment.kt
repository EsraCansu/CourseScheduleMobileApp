package com.university.courseschedule.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.university.courseschedule.R
import com.university.courseschedule.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        loadUserProfile()
        checkRegistration()
    }

    // ── Registration guard ────────────────────────────────────────────────────

    /**
     * Reads [isRegistered] from SharedPreferences.
     * If the user has not yet completed their profile a non-cancellable
     * [MaterialAlertDialogBuilder] dialog is shown; confirming redirects
     * them to [SettingsFragment].
     */
    private fun checkRegistration() {
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRegistered = prefs.getBoolean(KEY_IS_REGISTERED, false)

        if (!isRegistered) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_registration_title)
                .setMessage(R.string.dialog_registration_message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_go_to_settings) { dialog, _ ->
                    dialog.dismiss()
                    findNavController()
                        .navigate(R.id.action_homeFragment_to_settingsFragment)
                }
                .show()
        }
    }

    // ── Profile display ───────────────────────────────────────────────────────

    private fun loadUserProfile() {
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val name = prefs.getString(KEY_NAME, "").orEmpty()
        val surname = prefs.getString(KEY_SURNAME, "").orEmpty()
        val department = prefs.getString(KEY_DEPARTMENT, "").orEmpty()
        val role = prefs.getString(KEY_ROLE, "").orEmpty()

        binding.tvWelcome.text = when {
            name.isNotEmpty() || surname.isNotEmpty() ->
                getString(R.string.welcome_message, "$name $surname".trim())
            else -> getString(R.string.welcome_guest)
        }
        binding.tvDepartment.text = department
        binding.tvRole.text = role
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
