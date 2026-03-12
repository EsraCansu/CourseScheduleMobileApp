
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
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_IS_REGISTERED
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_ROLE
import com.university.courseschedule.ui.home.HomeFragment.Companion.PREFS_NAME

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
            
            // Check if user is logged in when navigating to home
            if (destination.id == R.id.homeFragment && !authManager.isLoggedIn()) {
                // User not logged in - navigate to SignIn
                navController.navigate(R.id.action_homeFragment_to_signInFragment)
            }
        }
    }

    /**
     * Enforces role-based tab visibility on every destination change.
     *
     * ADMIN   — all four tabs visible.
     * LECTURER — Data and Settings tabs hidden (spec §3 / §6).
     *
     * Tabs remain in their default (all-visible) state until the user
     * logs in, so the Settings tab is always reachable on first launch.
     */
    private fun applyRoleBasedNavVisibility() {
        // If not logged in, keep all tabs visible
        if (!authManager.isLoggedIn()) {
            // Ensure all tabs are visible
            with(binding.bottomNavigationView.menu) {
                findItem(R.id.dataFragment)?.isVisible = true
                findItem(R.id.settingsFragment)?.isVisible = true
            }
            return
        }

        val user = authManager.getCurrentUser()
        val isLecturer = user?.role == Role.LECTURER

        with(binding.bottomNavigationView.menu) {
            findItem(R.id.dataFragment)?.isVisible = !isLecturer
            findItem(R.id.settingsFragment)?.isVisible = !isLecturer
        }
    }
}
