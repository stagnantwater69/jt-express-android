package com.jtexpress.app

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
import android.content.Intent

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Receive data from RegisterActivity
        val registeredEmail = intent.getStringExtra("registered_email")
        val showSuccess = intent.getBooleanExtra("show_success", false)

        if (showSuccess && registeredEmail != null) {
            etEmail.setText(registeredEmail)
            Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_LONG).show()
        }

        initViews()
        setupClickListeners()
        styleSignUpText()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnGoogle = findViewById(R.id.btn_google)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvSignUp = findViewById(R.id.tv_signup)
    }

    private fun setupClickListeners() {
        // Login button
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        // Google login
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
            // TODO: Integrate Google Sign-In SDK
        }

        // Forgot password
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            intent.putExtra("prefill_email", etEmail.text.toString().trim())
            startActivity(intent)
        }

        // Sign Up
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(): Boolean {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        var isValid = true

        // Clear previous errors
        tilEmail.error = null
        tilPassword.error = null

        // Validate email/phone
        if (email.isEmpty()) {
            tilEmail.error = "Email or phone number is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() && !isValidPhone(email)) {
            tilEmail.error = "Enter a valid email or phone number"
            isValid = false
        }

        // Validate password
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun isValidPhone(phone: String): Boolean {
        return Patterns.PHONE.matcher(phone).matches() && phone.length >= 10
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Show loading state
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        // TODO: Replace with actual API call (Retrofit, Firebase, etc.)
        // Simulating network delay for demo
        btnLogin.postDelayed({
            // Demo: Accept any valid-format credentials
            // In production, verify against your backend
            saveLoginState(email)
            navigateToMain()
        }, 1500)
    }

    private fun saveLoginState(email: String) {
        val sharedPref = getSharedPreferences("JTExpressPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putString("user_email", email)
            apply()
        }
    }

    private fun navigateToMain() {
        // TODO: Replace with your MainActivity once created
        Toast.makeText(this, "Login Successful! Welcome to J&T Express", Toast.LENGTH_LONG).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        // Reset button for demo
        btnLogin.isEnabled = true
        btnLogin.text = "LOG IN"
    }

    private fun styleSignUpText() {
        val fullText = "Don't have an account? Sign Up"
        val spannable = SpannableString(fullText)
        val redColor = ContextCompat.getColor(this, R.color.jt_red)

        val signUpStart = fullText.indexOf("Sign Up")
        val signUpEnd = signUpStart + "Sign Up".length

        spannable.setSpan(
            ForegroundColorSpan(redColor),
            signUpStart,
            signUpEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvSignUp.text = spannable
    }
}