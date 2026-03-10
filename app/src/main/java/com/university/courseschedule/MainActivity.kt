package com.university.courseschedule

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.university.courseschedule.databinding.ActivityMainBinding
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_IS_REGISTERED
import com.university.courseschedule.ui.home.HomeFragment.Companion.KEY_ROLE
import com.university.courseschedule.ui.home.HomeFragment.Companion.PREFS_NAME

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire BottomNavigationView to the NavController.
        // Each menu item ID must match the corresponding fragment destination ID in nav_graph.xml.
        binding.bottomNavigationView.setupWithNavController(navController)

        // Apply role-based tab visibility on every destination change so it
        // stays in sync after the user saves their profile in Settings.
        navController.addOnDestinationChangedListener { _, _, _ ->
            applyRoleBasedNavVisibility()
        }
    }

    /**
     * Hides the "Data" tab for Lecturers (per spec §6).
     * Reads the persisted role from SharedPreferences; the tab is visible by
     * default so Admins always see it without extra work.
     */
    private fun applyRoleBasedNavVisibility() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRegistered = prefs.getBoolean(KEY_IS_REGISTERED, false)
        if (!isRegistered) return   // Don't adjust tabs until profile is saved.

        val role = prefs.getString(KEY_ROLE, "").orEmpty()
        val isLecturer = role.equals("Lecturer", ignoreCase = true)

        binding.bottomNavigationView.menu
            .findItem(R.id.dataFragment)
            ?.isVisible = !isLecturer
    }
}
