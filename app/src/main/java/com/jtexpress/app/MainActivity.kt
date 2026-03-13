package com.jtexpress.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        // Load Home fragment on start
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { loadFragment(HomeFragment()); true }
                R.id.nav_my_order -> { loadFragment(MyOrderFragment()); true }
                R.id.nav_rates    -> { loadFragment(RatesFragment()); true }
                R.id.nav_nearby   -> { loadFragment(NearbyFragment()); true }
                R.id.nav_me       -> { loadFragment(MeFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Called from fragments to switch tabs programmatically
    fun switchToTab(itemId: Int) {
        bottomNav.selectedItemId = itemId
    }

    override fun onBackPressed() {
        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
            finishAffinity()
        }
    }
}