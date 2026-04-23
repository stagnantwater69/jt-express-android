package com.jtexpress.app.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.jtexpress.app.AdminUserFragment
import com.jtexpress.app.shared.LoginActivity
import com.jtexpress.app.R


class AdminMainActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        if (user == null) { goToLogin(); return }

        setContentView(R.layout.activity_admin_main)
        bottomNav = findViewById(R.id.bottom_nav_admin)

        if (savedInstanceState == null) {
            loadFragment(AdminHomeFragment())
        }

        bottomNav!!.setOnItemSelectedListener { item ->
            clearBackStack()
            when (item.itemId) {
                R.id.admin_nav_home     -> { loadFragment(AdminHomeFragment());     true }
                R.id.admin_nav_users    -> { loadFragment(AdminUserFragment());     true }
                R.id.admin_nav_orders   -> { loadFragment(AdminOrderFragment());    true }
                R.id.admin_nav_reports  -> { loadFragment(AdminReportsFragment());  true }
                R.id.admin_nav_settings -> { loadFragment(AdminSettingsFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }

    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
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
        if (bottomNav!!.selectedItemId != R.id.admin_nav_home) {
            bottomNav!!.selectedItemId = R.id.admin_nav_home
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