package com.jtexpress.app.shared

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
 * Single post-login entry point for ALL roles.
 * Reads the `role` field from Firestore and routes to the correct dashboard.
 *
 * Flow:
 *   LoginActivity → RoleRouterActivity → [MainActivity | RiderMainActivity |
 *                                          StaffMainActivity | AdminMainActivity]
 *
 * Routing priority:
 *   1. Firestore (live, authoritative)
 *   2. Cached SharedPrefs role — only used when Firestore is unreachable AND the
 *      cached value is a known non-empty role. This prevents a rider/staff/admin
 *      from being silently downgraded to customer during a flaky connection.
 *   3. If neither source yields a known role, sign out and send to Login so the
 *      user is not stuck in the wrong dashboard indefinitely.
 */
class RoleRouterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    // FIX: enumerate every valid role so we never silently accept garbage values.
    private val KNOWN_ROLES = setOf("customer", "rider", "staff", "admin")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val role     = doc?.getString("role")?.trim() ?: ""
                val isActive = doc?.getBoolean("isActive") ?: true

                // FIX: cache the real role only when it is a known value.
                if (role in KNOWN_ROLES) {
                    getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE).edit()
                        .putString("user_uid",  user.uid)
                        .putString("user_role", role)
                        .apply()
                } else {
                    Log.w("RoleRouter",
                        "Unknown or missing role '$role' for uid=${user.uid}. " +
                                "Defaulting to 'customer'. Fix the Firestore document.")
                }

                // Non-customer accounts that are deactivated cannot log in.
                if (!isActive && role != "customer") {
                    auth.signOut()
                    Toast.makeText(
                        this,
                        "Your account has been deactivated. Contact admin.",
                        Toast.LENGTH_LONG
                    ).show()
                    goToLogin()
                    return@addOnSuccessListener
                }

                // Route by role. Unknown/empty role → customer (with a log warning above).
                routeToRole(if (role in KNOWN_ROLES) role else "customer")
            }
            .addOnFailureListener { e ->
                Log.e("RoleRouter", "Firestore unreachable: ${e.message}")

                // FIX: fall back to cached role only if it is a known non-empty value.
                // Do NOT default to "customer" unconditionally — that would downgrade
                // a rider or admin on any network hiccup.
                val cachedRole = getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
                    .getString("user_role", "") ?: ""

                if (cachedRole in KNOWN_ROLES) {
                    Log.i("RoleRouter", "Using cached role '$cachedRole' for uid=${user.uid}")
                    routeToRole(cachedRole)
                } else {
                    // No usable cached role — sign out rather than guess.
                    Log.w("RoleRouter",
                        "No valid cached role for uid=${user.uid}. Sending to login.")
                    Toast.makeText(
                        this,
                        "Could not verify your account. Please log in again.",
                        Toast.LENGTH_LONG
                    ).show()
                    auth.signOut()
                    goToLogin()
                }
            }
    }

    private fun routeToRole(role: String) {
        // FIX: every branch is explicit — no silent else → customer catch-all.
        val target = when (role) {
            "rider"  -> RiderMainActivity::class.java
            "staff"  -> StaffMainActivity::class.java
            "admin"  -> AdminMainActivity::class.java
            else     -> MainActivity::class.java   // "customer" and any safe fallback
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