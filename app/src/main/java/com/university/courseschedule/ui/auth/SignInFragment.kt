package com.university.courseschedule.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.university.courseschedule.R
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.data.FirebaseAuthManager
import com.university.courseschedule.databinding.FragmentSigninBinding
import kotlinx.coroutines.launch

/**
 * SignInSheet - A dismissible BottomSheetDialogFragment for user authentication.
 *
 * Authentication order:
 *   1. Firebase Auth (primary — requires network + google-services.json)
 *   2. Local SharedPrefs AuthManager (fallback — works offline)
 */
class SignInFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SignInSheet"
    }

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    private lateinit var authManager: AuthManager
    private lateinit var firebaseAuth: FirebaseAuthManager

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
        authManager  = AuthManager.getInstance(requireContext())
        firebaseAuth = FirebaseAuthManager.getInstance()
        setupTextValidation()
        setupClickListeners()
    }

    /**
     * Sign In button enabled only when Email format is valid and Password >= 6 chars.
     */
    private fun setupTextValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateForm() }
        }
        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
        validateForm()
    }

    private fun validateForm() {
        val isEmailValid    = android.util.Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString().trim()).matches()
        val isPasswordValid = binding.etPassword.text.toString().length >= 6
        binding.btnSignIn.isEnabled = isEmailValid && isPasswordValid
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener { performSignIn() }

        binding.tvSignUp.setOnClickListener {
            dismiss()
            SignUpFragment().show(parentFragmentManager, SignUpFragment.TAG)
        }
    }

    private fun performSignIn() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Field validation
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

        // Show loading state
        setLoading(true)

        lifecycleScope.launch {
            // 1️⃣ Try Firebase Auth (primary)
            val firebaseUser = firebaseAuth.signIn(email, password)

            if (firebaseUser != null) {
                val profile = firebaseAuth.getUserProfile(firebaseUser.uid)
                if (profile != null) {
                    // Mirror UID into local session so role-based nav works
                    authManager.setCurrentUserId(firebaseUser.uid)
                    onSignInSuccess(profile.fullName)
                } else {
                    // Firebase auth OK but no Firestore doc — fall back to local
                    localSignIn(email, password)
                }
            } else {
                // 2️⃣ Firebase failed (offline / wrong creds) — try local SharedPrefs
                localSignIn(email, password)
            }

            setLoading(false)
        }
    }

    /** Falls back to the local SharedPrefs AuthManager when Firebase is unavailable. */
    private fun localSignIn(email: String, password: String) {
        val user = authManager.signIn(email, password)
        if (user != null) {
            authManager.setCurrentUserId(user.id)
            onSignInSuccess(user.fullName)
        } else {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSignInSuccess(displayName: String) {
        Toast.makeText(requireContext(), getString(R.string.welcome_message, displayName), Toast.LENGTH_SHORT).show()
        dismiss()
        @Suppress("DEPRECATION")
        targetFragment?.onResume()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.btnSignIn.text = if (loading) getString(R.string.signing_in) else getString(R.string.btn_sign_in)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
