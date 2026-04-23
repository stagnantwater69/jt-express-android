package com.jtexpress.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.jtexpress.app.shared.LoginActivity
import com.jtexpress.app.staff.StaffApprovalFragment
import com.jtexpress.app.staff.StaffHomeFragment
import com.jtexpress.app.staff.StaffOrderFragment
import com.jtexpress.app.staff.StaffProfileFragment
import com.jtexpress.app.staff.StaffRiderFragment

class StaffMainActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        if (user == null) { goToLogin(); return }

        setContentView(R.layout.activity_staff_main)
        bottomNav = findViewById(R.id.bottom_nav_staff)

        if (savedInstanceState == null) {
            loadFragment(StaffHomeFragment())
        }

        bottomNav!!.setOnItemSelectedListener { item ->
            clearBackStack()
            when (item.itemId) {
                R.id.staff_nav_home     -> { loadFragment(StaffHomeFragment());     true }
                R.id.staff_nav_orders   -> { loadFragment(StaffOrderFragment());    true }
                R.id.staff_nav_approval -> { loadFragment(StaffApprovalFragment()); true }
                R.id.staff_nav_riders   -> { loadFragment(StaffRiderFragment());    true }
                R.id.staff_nav_profile  -> { loadFragment(StaffProfileFragment());  true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.staff_fragment_container, fragment)
            .commit()
    }

    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.staff_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun clearBackStack() {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStackImmediate(
                fm.getBackStackEntryAt(0).id,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(); return
        }
        if (bottomNav!!.selectedItemId != R.id.staff_nav_home) {
            bottomNav!!.selectedItemId = R.id.staff_nav_home
        } else {
            super.onBackPressed(); finishAffinity()
        }
    }

    fun logout() {
        getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        auth.signOut()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}