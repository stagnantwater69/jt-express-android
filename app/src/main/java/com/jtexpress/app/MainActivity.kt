package com.jtexpress.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ Check if user is logged in — if not, go to Login
        val currentUser = auth!!.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        // ✅ Check if email is verified — if not, sign out and go to Login
        if (!currentUser.isEmailVerified) {
            auth!!.signOut()
            goToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        // Load Home fragment on start
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav!!.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { loadFragment(HomeFragment()); true }
                R.id.nav_my_order -> { loadFragment(MyOrderFragment()); true }
                R.id.nav_rates    -> { loadFragment(RatesFragment()); true }
                R.id.nav_nearby   -> { loadFragment(NearbyFragment()); true }
                R.id.nav_me       -> { loadFragment(MeFragment()); true }
                else -> false
            }
        }

        // Remove test Firestore call if present
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun switchToTab(itemId: Int) {
        bottomNav!!.selectedItemId = itemId
    }

    // ✅ Called from MeFragment logout
    fun logout() {
        // Sign out from Firebase
        auth!!.signOut()

        // Clear SharedPreferences
        val prefs = getSharedPreferences("JTExpressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()

        // Go to Login
        goToLogin()
    }

    private fun goToLogin() {
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    override fun onBackPressed() {
        if (bottomNav!!.selectedItemId != R.id.nav_home) {
            bottomNav!!.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
            finishAffinity()
        }
    }
}