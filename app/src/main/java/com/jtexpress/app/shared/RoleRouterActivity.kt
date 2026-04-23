package com.jtexpress.app.shared

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.MainActivity
import com.jtexpress.app.StaffMainActivity
import com.jtexpress.app.admin.AdminMainActivity
import com.jtexpress.app.rider.RiderMainActivity

/**
 * RoleRouterActivity
 *
 * This is the ONLY post-login entry point for ALL roles.
 * It reads the `role` field from Firestore and routes to the correct dashboard.
 *
 * Flow:
 *   LoginActivity → RoleRouterActivity → [MainActivity | RiderMainActivity | StaffMainActivity | AdminMainActivity]
 *
 * Never navigate directly from LoginActivity to any dashboard.
 */
class RoleRouterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        // Read role from Firestore
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val role     = doc?.getString("role") ?: "customer"
                val isActive = doc?.getBoolean("isActive") ?: true

                // Save role and uid to SharedPreferences for quick access
                getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE).edit()
                    .putString("user_uid",  user.uid)
                    .putString("user_role", role)
                    .apply()

                if (!isActive && role != "customer") {
                    // Non-customer accounts that are deactivated cannot log in
                    auth.signOut()
                    Toast.makeText(this, "Your account has been deactivated. Contact admin.", Toast.LENGTH_LONG).show()
                    goToLogin()
                    return@addOnSuccessListener
                }

                routeToRole(role)
            }
            .addOnFailureListener {
                // Firestore unreachable — fall back to cached role
                val cachedRole = getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
                    .getString("user_role", "customer") ?: "customer"
                routeToRole(cachedRole)
            }
    }

    private fun routeToRole(role: String) {
        val target = when (role) {
            "rider" -> RiderMainActivity::class.java
            "staff" -> StaffMainActivity::class.java
            "admin" -> AdminMainActivity::class.java
            else    -> MainActivity::class.java  // "customer" is the default
        }

        startActivity(
            Intent(this, target).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}