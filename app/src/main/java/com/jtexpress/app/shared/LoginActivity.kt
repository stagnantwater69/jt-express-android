package com.jtexpress.app.shared

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R
import com.jtexpress.app.shared.ForgotPasswordActivity
import com.jtexpress.app.shared.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    private var tilEmail: TextInputLayout? = null
    private var tilPassword: TextInputLayout? = null
    private var etEmail: TextInputEditText? = null
    private var etPassword: TextInputEditText? = null
    private var btnLogin: MaterialButton? = null
    private var btnGoogle: MaterialButton? = null
    private var tvForgotPassword: TextView? = null
    private var tvSignUp: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        // Already logged in and verified → go to RoleRouter (not MainActivity directly)
        val currentUser = auth!!.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            navigateToRoleRouter()
            return
        }

        if (currentUser != null && !currentUser.isEmailVerified) {
            auth!!.signOut()
        }

        initViews()
        setupClickListeners()
        styleSignUpText()

        val registeredEmail = intent.getStringExtra("registered_email")
        val showSuccess     = intent.getBooleanExtra("show_success", false)
        if (showSuccess && registeredEmail != null) {
            etEmail!!.setText(registeredEmail)
            Toast.makeText(this, "Email verified! Please log in.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        tilEmail         = findViewById(R.id.til_email)
        tilPassword      = findViewById(R.id.til_password)
        etEmail          = findViewById(R.id.et_email)
        etPassword       = findViewById(R.id.et_password)
        btnLogin         = findViewById(R.id.btn_login)
        btnGoogle        = findViewById(R.id.btn_google)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvSignUp         = findViewById(R.id.tv_signup)
    }

    private fun setupClickListeners() {
        btnLogin!!.setOnClickListener {
            if (validateInputs()) performLogin()
        }
        btnGoogle!!.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }
        tvForgotPassword!!.setOnClickListener {
            val i = Intent(this, ForgotPasswordActivity::class.java)
            val emailText = etEmail!!.text
            if (emailText != null) i.putExtra("prefill_email", emailText.toString())
            startActivity(i)
        }
        tvSignUp!!.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun manualTrim(input: String): String {
        var start = 0; var end = input.length
        while (start < end && input[start] == ' ') start++
        while (end > start && input[end - 1] == ' ') end--
        return input.substring(start, end)
    }

    private fun validateInputs(): Boolean {
        val email    = manualTrim(etEmail!!.text?.toString() ?: "")
        val password = manualTrim(etPassword!!.text?.toString() ?: "")
        var isValid  = true

        tilEmail!!.error = null
        tilPassword!!.error = null

        if (email.isEmpty()) {
            tilEmail!!.error = "Email is required"; isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail!!.error = "Enter a valid email address"; isValid = false
        }
        if (password.isEmpty()) {
            tilPassword!!.error = "Password is required"; isValid = false
        } else if (password.length < 6) {
            tilPassword!!.error = "Password must be at least 6 characters"; isValid = false
        }
        return isValid
    }

    private fun performLogin() {
        val email    = manualTrim(etEmail!!.text.toString())
        val password = manualTrim(etPassword!!.text.toString())

        btnLogin!!.isEnabled = false
        btnLogin!!.text      = "Logging in..."

        auth!!.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user!!
                if (!user.isEmailVerified) {
                    auth!!.signOut()
                    btnLogin!!.isEnabled = true
                    btnLogin!!.text      = "LOG IN"
                    tilEmail!!.error     = "Email not verified. Please check your inbox."
                    return@addOnSuccessListener
                }

                // Fetch Firestore data, save to prefs, then route via RoleRouterActivity
                db!!.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val firstName  = doc.getString("firstName") ?: ""
                            val middleName = doc.getString("middleName") ?: ""
                            val lastName   = doc.getString("lastName") ?: ""
                            val fullName   = if (middleName.isEmpty()) "$firstName $lastName"
                            else "$firstName $middleName $lastName"
                            val displayName = manualTrim(fullName)
                            val phone = doc.getString("phone") ?: ""
                            val role  = doc.getString("role") ?: "customer"
                            saveUserToPrefs(email, displayName, phone, role, user.uid)
                        } else {
                            saveUserToPrefs(email, "", "", "customer", user.uid)
                        }
                        navigateToRoleRouter()
                    }
                    .addOnFailureListener {
                        saveUserToPrefs(email, "", "", "customer", user.uid)
                        navigateToRoleRouter()
                    }
            }
            .addOnFailureListener { e ->
                btnLogin!!.isEnabled = true
                btnLogin!!.text      = "LOG IN"
                val msg = e.message ?: ""
                tilPassword!!.error = when {
                    msg.contains("no user record")    -> "No account found with this email."
                    msg.contains("password is invalid") -> "Incorrect password. Try again."
                    msg.contains("blocked")           -> "Too many attempts. Try again later."
                    msg.contains("network")           -> "Network error. Check your connection."
                    else                              -> "Login failed. Please try again."
                }
            }
    }

    private fun saveUserToPrefs(email: String, name: String, phone: String, role: String, uid: String) {
        getSharedPreferences("JTExpressPrefs", MODE_PRIVATE).edit()
            .putBoolean("is_logged_in", true)
            .putString("user_uid",   uid)
            .putString("user_email", email)
            .putString("user_name",  name)
            .putString("user_phone", phone)
            .putString("user_role",  role)
            .apply()
    }

    // KEY CHANGE: routes to RoleRouterActivity, not MainActivity
    private fun navigateToRoleRouter() {
        startActivity(
            Intent(this, RoleRouterActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun styleSignUpText() {
        val fullText  = "Don't have an account? Sign Up"
        val spannable = SpannableString(fullText)
        val redColor  = ContextCompat.getColor(this, R.color.jt_red)
        val start     = fullText.indexOf("Sign Up")
        spannable.setSpan(ForegroundColorSpan(redColor), start, start + 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSignUp!!.text = spannable
    }
}