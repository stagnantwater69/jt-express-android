package com.jtexpress.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.shared.LoginActivity

class MainActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        // Check if user is logged in — if not, go to Login
        val currentUser = auth!!.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        // Check if email is verified — if not, sign out and go to Login
        if (!currentUser.isEmailVerified) {
            auth!!.signOut()
            goToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        // Load Home fragment on start
        if (savedInstanceState == null) {
            loadRootFragment(HomeFragment())
        }

        bottomNav!!.setOnItemSelectedListener { item ->
            // If we're on a sub-screen (e.g. CreateOrderFragment), clear back stack first
            clearBackStack()

            when (item.itemId) {
                R.id.nav_home     -> { loadRootFragment(HomeFragment());     true }
                R.id.nav_my_order -> { loadRootFragment(MyOrderFragment());  true }
                R.id.nav_rates    -> { loadRootFragment(RatesFragment());    true }
                R.id.nav_nearby   -> { loadRootFragment(NearbyFragment());   true }
                R.id.nav_me       -> { loadRootFragment(MeFragment());       true }
                else -> false
            }
        }

        OverlayPermissionActivity.requestIfNeeded(this)
    }

    // ── Root fragment (no back stack) ─────────────────────────────
    private fun loadRootFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ── Sub-screen fragment (with back stack) ─────────────────────
    // Use this for screens launched from within a tab (e.g. Create Order)
    fun navigateTo(fragment: Fragment, tag: String? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    // ── Navigate to Create Order ──────────────────────────────────
    fun openCreateOrder() {
        navigateTo(CreateOrderFragment(), "create_order")
    }

    // ── Switch bottom nav tab ─────────────────────────────────────
    fun switchToTab(itemId: Int) {
        clearBackStack()
        bottomNav!!.selectedItemId = itemId
    }

    // ── Clear back stack (sub-screens) ────────────────────────────
    private fun clearBackStack() {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStackImmediate(
                fm.getBackStackEntryAt(0).id,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    // ── Back press ────────────────────────────────────────────────
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val fm = supportFragmentManager

        // If there are sub-screens on the stack, pop them first
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            return
        }

        // If we're not on Home tab, go back to Home
        if (bottomNav!!.selectedItemId != R.id.nav_home) {
            bottomNav!!.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
            finishAffinity()
        }
    }

    // ── Logout (called from MeFragment) ──────────────────────────
    fun logout() {
        auth!!.signOut()

        val prefs = getSharedPreferences("JTExpressPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        goToLogin()
    }

    private fun goToLogin() {
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }
}