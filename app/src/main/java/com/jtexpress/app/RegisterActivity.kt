package com.jtexpress.app

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
        styleLoginText()
    }

    private fun initViews() {
        tilFullName = findViewById(R.id.til_fullname)
        tilEmail = findViewById(R.id.til_email)
        tilPhone = findViewById(R.id.til_phone)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)

        etFullName = findViewById(R.id.et_fullname)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)

        cbTerms = findViewById(R.id.cb_terms)
        btnRegister = findViewById(R.id.btn_register)
        btnBack = findViewById(R.id.btn_back)
        tvLogin = findViewById(R.id.tv_login)
    }

    private fun setupClickListeners() {
        // Back button → go back to Login
        btnBack.setOnClickListener {
            finish()
        }

        // Register button
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegister()
            }
        }

        // Go to Login
        tvLogin.setOnClickListener {
            finish() // goes back to LoginActivity
        }
    }

    private fun validateInputs(): Boolean {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        var isValid = true

        // Clear errors
        tilFullName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (fullName.isEmpty()) {
            tilFullName.error = "Full name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (phone.isEmpty()) {
            tilPhone.error = "Phone number is required"
            isValid = false
        } else if (phone.length < 10) {
            tilPhone.error = "Enter a valid phone number"
            isValid = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun performRegister() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()

        btnRegister.isEnabled = false
        btnRegister.text = "Creating account..."

        // Simulate network delay
        btnRegister.postDelayed({
            // ✅ Pass data back to LoginActivity using Intent
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("registered_email", email)
            intent.putExtra("registered_name", fullName)
            intent.putExtra("show_success", true)
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun styleLoginText() {
        val fullText = "Already have an account? Log In"
        val spannable = SpannableString(fullText)
        val redColor = ContextCompat.getColor(this, R.color.jt_red)

        val start = fullText.indexOf("Log In")
        val end = start + "Log In".length

        spannable.setSpan(
            ForegroundColorSpan(redColor),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvLogin.text = spannable
    }
}