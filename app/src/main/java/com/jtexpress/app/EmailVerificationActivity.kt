package com.jtexpress.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmailVerificationActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    private var tvEmailDisplay: TextView? = null
    private var tvResendTimer: TextView? = null
    private var btnVerified: MaterialButton? = null
    private var btnResend: MaterialButton? = null
    private var tvBackToLogin: TextView? = null
    private var btnBack: ImageButton? = null

    private var resendTimer: CountDownTimer? = null
    private var autoCheckHandler: Handler? = null
    private var autoCheckRunnable: Runnable? = null

    private var userEmail: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        userEmail = intent.getStringExtra("registered_email") ?: ""
        userName  = intent.getStringExtra("registered_name") ?: ""

        initViews()
        setupClickListeners()
        startAutoCheck()
    }

    private fun initViews() {
        tvEmailDisplay = findViewById(R.id.tv_email_display)
        tvResendTimer  = findViewById(R.id.tv_resend_timer)
        btnVerified    = findViewById(R.id.btn_verified)
        btnResend      = findViewById(R.id.btn_resend)
        tvBackToLogin  = findViewById(R.id.tv_back_to_login)
        btnBack        = findViewById(R.id.btn_back)

        if (userEmail.isNotEmpty()) {
            tvEmailDisplay!!.text = userEmail
        }
    }

    private fun setupClickListeners() {
        btnBack!!.setOnClickListener { signOutAndGoToLogin() }
        btnVerified!!.setOnClickListener { checkVerification() }
        btnResend!!.setOnClickListener { resendVerificationEmail() }
        tvBackToLogin!!.setOnClickListener { signOutAndGoToLogin() }
    }

    // ── Check if email is verified ───────────────────────────────
    private fun checkVerification() {
        val user = auth!!.currentUser
        if (user == null) {
            signOutAndGoToLogin()
            return
        }

        btnVerified!!.isEnabled = false
        btnVerified!!.text = "Checking..."

        user.reload()
            .addOnSuccessListener {
                val freshUser = auth!!.currentUser
                if (freshUser != null && freshUser.isEmailVerified) {
                    stopAutoCheck()
                    // ✅ Fetch user data from Firestore BEFORE going to Main
                    fetchUserDataAndProceed(freshUser.uid)
                } else {
                    btnVerified!!.isEnabled = true
                    btnVerified!!.text = "I'VE VERIFIED MY EMAIL"
                    Toast.makeText(this,
                        "Email not verified yet. Please check your inbox.",
                        Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                btnVerified!!.isEnabled = true
                btnVerified!!.text = "I'VE VERIFIED MY EMAIL"
                Toast.makeText(this,
                    "Could not check verification. Try again.",
                    Toast.LENGTH_SHORT).show()
            }
    }

    // ── Fetch Firestore data then save to prefs ──────────────────
    private fun fetchUserDataAndProceed(uid: String) {
        btnVerified!!.text = "Loading your profile..."

        db!!.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val firstName  = doc.getString("firstName") ?: ""
                    val middleName = doc.getString("middleName") ?: ""
                    val lastName   = doc.getString("lastName") ?: ""
                    val email      = doc.getString("email") ?: userEmail
                    val phone      = doc.getString("phone") ?: ""
                    val role       = doc.getString("role") ?: "customer"

                    // Build full name
                    val fullName: String
                    if (middleName.isEmpty()) {
                        fullName = "$firstName $lastName"
                    } else {
                        fullName = "$firstName $middleName $lastName"
                    }

                    val displayName = manualTrim(fullName)
                    saveUserToPrefs(email, displayName, phone, role, uid)
                } else {
                    // No Firestore doc — save what we have from registration
                    saveUserToPrefs(userEmail, userName, "", "customer", uid)
                }
                goToLogin()
            }
            .addOnFailureListener {
                // Firestore failed — use registration data as fallback
                saveUserToPrefs(userEmail, userName, "", "customer", uid)
                goToLogin()
            }
    }

    // ── Save to SharedPreferences ────────────────────────────────
    private fun saveUserToPrefs(
        email: String,
        name: String,
        phone: String,
        role: String,
        uid: String
    ) {
        val prefs: SharedPreferences = getSharedPreferences("JTExpressPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putString("user_uid",   uid)
        editor.putString("user_email", email)
        editor.putString("user_name",  name)
        editor.putString("user_phone", phone)
        editor.putString("user_role",  role)
        editor.apply()
    }

    // ── Manual trim ──────────────────────────────────────────────
    private fun manualTrim(input: String): String {
        var start = 0
        var end = input.length
        while (start < end && input[start] == ' ') start++
        while (end > start && input[end - 1] == ' ') end--
        return input.substring(start, end)
    }

    // ── Resend verification email ────────────────────────────────
    private fun resendVerificationEmail() {
        val user = auth!!.currentUser
        if (user == null) {
            signOutAndGoToLogin()
            return
        }

        btnResend!!.isEnabled = false

        user.sendEmailVerification()
            .addOnSuccessListener {
                Toast.makeText(this,
                    "Verification email resent! Check your inbox.",
                    Toast.LENGTH_LONG).show()
                startResendCooldown()
            }
            .addOnFailureListener {
                btnResend!!.isEnabled = true
                Toast.makeText(this,
                    "Failed to resend. Try again later.",
                    Toast.LENGTH_SHORT).show()
            }
    }

    // ── 60s resend cooldown timer ────────────────────────────────
    private fun startResendCooldown() {
        tvResendTimer!!.visibility = View.VISIBLE

        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvResendTimer!!.text = "Resend available in ${seconds}s"
            }
            override fun onFinish() {
                btnResend!!.isEnabled = true
                tvResendTimer!!.visibility = View.GONE
                tvResendTimer!!.text = ""
            }
        }.start()
    }

    // ── Auto-check every 5 seconds ───────────────────────────────
    private fun startAutoCheck() {
        autoCheckHandler = Handler(Looper.getMainLooper())
        autoCheckRunnable = object : Runnable {
            override fun run() {
                val user = auth!!.currentUser
                if (user != null) {
                    user.reload().addOnSuccessListener {
                        val freshUser = auth!!.currentUser
                        if (freshUser != null && freshUser.isEmailVerified) {
                            stopAutoCheck()
                            fetchUserDataAndProceed(freshUser.uid)
                        }
                    }
                }
                autoCheckHandler!!.postDelayed(this, 5000)
            }
        }
        autoCheckHandler!!.postDelayed(autoCheckRunnable!!, 5000)
    }

    private fun stopAutoCheck() {
        if (autoCheckHandler != null && autoCheckRunnable != null) {
            autoCheckHandler!!.removeCallbacks(autoCheckRunnable!!)
        }
    }

    // ── Navigate to Login with success message ───────────────────
    private fun goToLogin() {
        Toast.makeText(this,
            "Email verified! Please log in.", Toast.LENGTH_LONG).show()
        val i = Intent(this, LoginActivity::class.java)
        i.putExtra("registered_email", userEmail)
        i.putExtra("show_success", true)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }

    private fun signOutAndGoToLogin() {
        auth!!.signOut()
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoCheck()
        resendTimer?.cancel()
    }
}