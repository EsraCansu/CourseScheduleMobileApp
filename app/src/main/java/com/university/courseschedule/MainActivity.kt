
package com.university.courseschedule

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.university.courseschedule.data.AuthManager
import com.university.courseschedule.databinding.ActivityMainBinding
import com.university.courseschedule.data.model.Role

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire BottomNavigationView to the NavController.
        // Each menu item ID must match the corresponding fragment destination ID in nav_graph.xml.
        binding.bottomNavigationView.setupWithNavController(navController)

        // Apply role-based tab visibility on every destination change so it
        // stays in sync after the user saves their profile in Settings.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            applyRoleBasedNavVisibility()
            // Note: Do NOT auto-redirect guests from Home to SignIn.
            // HomeFragment handles showing the Guest UI when not logged in.
            // Auto-redirecting breaks the Guest View requirement (spec §3.1).
        }
    }

    /**
     * Enforces role-based tab visibility on every destination change.
     *
     * ADMIN   — all four tabs visible.
     * LECTURER — only Data tab hidden (spec §3.3: Admin-only).
     *            Settings tab remains visible so lecturers can manage their profile.
     *
     * Tabs remain in their default (all-visible) state until the user
     * logs in, so the Settings tab is always reachable on first launch.
     */
    private fun applyRoleBasedNavVisibility() {
        // If not logged in, keep all tabs visible
        if (!authManager.isLoggedIn()) {
            with(binding.bottomNavigationView.menu) {
                findItem(R.id.dataFragment)?.isVisible = true
                findItem(R.id.settingsFragment)?.isVisible = true
            }
            return
        }

        val user = authManager.getCurrentUser()
        val isLecturer = user?.role == Role.LECTURER

        with(binding.bottomNavigationView.menu) {
            // Only Data tab is Admin-only (spec §3.3)
            findItem(R.id.dataFragment)?.isVisible = !isLecturer
            // Settings is available for ALL roles (spec §3.4)
            findItem(R.id.settingsFragment)?.isVisible = true
        }
    }
}
