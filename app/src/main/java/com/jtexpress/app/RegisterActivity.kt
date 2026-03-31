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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    // Name fields
    private var tilFirstName: TextInputLayout? = null
    private var tilMiddleName: TextInputLayout? = null
    private var tilLastName: TextInputLayout? = null

    // Other fields
    private var tilEmail: TextInputLayout? = null
    private var tilPhone: TextInputLayout? = null
    private var tilPassword: TextInputLayout? = null
    private var tilConfirmPassword: TextInputLayout? = null

    private var etFirstName: TextInputEditText? = null
    private var etMiddleName: TextInputEditText? = null
    private var etLastName: TextInputEditText? = null
    private var etEmail: TextInputEditText? = null
    private var etPhone: TextInputEditText? = null
    private var etPassword: TextInputEditText? = null
    private var etConfirmPassword: TextInputEditText? = null

    private var cbTerms: CheckBox? = null
    private var btnRegister: MaterialButton? = null
    private var btnBack: ImageButton? = null
    private var tvLogin: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()
        styleLoginText()
    }

    private fun initViews() {
        // Name
        tilFirstName  = findViewById(R.id.til_firstname)
        tilMiddleName = findViewById(R.id.til_middlename)
        tilLastName   = findViewById(R.id.til_lastname)

        etFirstName  = findViewById(R.id.et_firstname)
        etMiddleName = findViewById(R.id.et_middlename)
        etLastName   = findViewById(R.id.et_lastname)

        // Other
        tilEmail           = findViewById(R.id.til_email)
        tilPhone           = findViewById(R.id.til_phone)
        tilPassword        = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)

        etEmail           = findViewById(R.id.et_email)
        etPhone           = findViewById(R.id.et_phone)
        etPassword        = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)

        cbTerms     = findViewById(R.id.cb_terms)
        btnRegister = findViewById(R.id.btn_register)
        btnBack     = findViewById(R.id.btn_back)
        tvLogin     = findViewById(R.id.tv_login)
    }

    private fun setupClickListeners() {
        btnBack!!.setOnClickListener { finish() }
        btnRegister!!.setOnClickListener {
            if (validateInputs()) performRegister()
        }
        tvLogin!!.setOnClickListener { finish() }
    }

    // ── Manual trim ──────────────────────────────────────────────
    private fun manualTrim(input: String): String {
        var start = 0
        var end = input.length
        while (start < end && input[start] == ' ') start++
        while (end > start && input[end - 1] == ' ') end--
        return input.substring(start, end)
    }

    // ── Validation ───────────────────────────────────────────────
    private fun validateInputs(): Boolean {
        val firstName   = manualTrim(if (etFirstName!!.text != null) etFirstName!!.text.toString() else "")
        val middleName  = manualTrim(if (etMiddleName!!.text != null) etMiddleName!!.text.toString() else "")
        val lastName    = manualTrim(if (etLastName!!.text != null) etLastName!!.text.toString() else "")
        val email       = manualTrim(if (etEmail!!.text != null) etEmail!!.text.toString() else "")
        val phone       = manualTrim(if (etPhone!!.text != null) etPhone!!.text.toString() else "")
        val password    = manualTrim(if (etPassword!!.text != null) etPassword!!.text.toString() else "")
        val confirmPass = manualTrim(if (etConfirmPassword!!.text != null) etConfirmPassword!!.text.toString() else "")

        var isValid = true

        // Clear all errors
        tilFirstName!!.error  = null
        tilMiddleName!!.error = null
        tilLastName!!.error   = null
        tilEmail!!.error      = null
        tilPhone!!.error      = null
        tilPassword!!.error   = null
        tilConfirmPassword!!.error = null

        // First name
        if (firstName.isEmpty()) {
            tilFirstName!!.error = "First name is required"
            isValid = false
        } else if (firstName.length < 2) {
            tilFirstName!!.error = "Enter a valid first name"
            isValid = false
        }

        // Middle name — OPTIONAL, only validate if not empty
        if (middleName.isNotEmpty() && middleName.length < 2) {
            tilMiddleName!!.error = "Enter a valid middle name"
            isValid = false
        }

        // Last name
        if (lastName.isEmpty()) {
            tilLastName!!.error = "Last name is required"
            isValid = false
        } else if (lastName.length < 2) {
            tilLastName!!.error = "Enter a valid last name"
            isValid = false
        }

        // Email
        if (email.isEmpty()) {
            tilEmail!!.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail!!.error = "Enter a valid email address"
            isValid = false
        }

        // Phone
        if (phone.isEmpty()) {
            tilPhone!!.error = "Phone number is required"
            isValid = false
        } else if (phone.length < 10) {
            tilPhone!!.error = "Enter a valid phone number (min 10 digits)"
            isValid = false
        }

        // Password
        if (password.isEmpty()) {
            tilPassword!!.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword!!.error = "Password must be at least 6 characters"
            isValid = false
        }

        // Confirm password
        if (confirmPass.isEmpty()) {
            tilConfirmPassword!!.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPass) {
            tilConfirmPassword!!.error = "Passwords do not match"
            isValid = false
        }

        // Terms
        if (!cbTerms!!.isChecked) {
            Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    // ── Firebase Register ────────────────────────────────────────
    private fun performRegister() {
        val firstName  = manualTrim(etFirstName!!.text.toString())
        val middleName = manualTrim(etMiddleName!!.text.toString())
        val lastName   = manualTrim(etLastName!!.text.toString())
        val email      = manualTrim(etEmail!!.text.toString())
        val phone      = manualTrim(etPhone!!.text.toString())
        val password   = manualTrim(etPassword!!.text.toString())

        // Build full name for display
        val fullName: String
        if (middleName.isEmpty()) {
            fullName = "$firstName $lastName"
        } else {
            fullName = "$firstName $middleName $lastName"
        }

        btnRegister!!.isEnabled = false
        btnRegister!!.text = "Creating account..."

        // Step 1: Create Firebase Auth user
        auth!!.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid

                // Step 2: Send verification email
                result.user!!.sendEmailVerification()
                    .addOnSuccessListener {
                        android.util.Log.d("AUTH", "Verification email sent to $email")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("AUTH", "Failed to send verification: ${e.message}")
                    }

                // Step 3: Save all name parts separately + full name to Firestore
                val userDoc = HashMap<String, Any>()
                userDoc["uid"]        = uid
                userDoc["firstName"]  = firstName
                userDoc["middleName"] = middleName   // empty string if not provided
                userDoc["lastName"]   = lastName
                userDoc["fullName"]   = fullName     // combined for display
                userDoc["email"]      = email
                userDoc["phone"]      = phone
                userDoc["role"]       = "customer"
                userDoc["carrots"]    = 0
                userDoc["vouchers"]   = 0
                userDoc["createdAt"]  = Timestamp.now()

                // Step 4: Save to Firestore
                db!!.collection("users").document(uid)
                    .set(userDoc)
                    .addOnSuccessListener {
                        val i = Intent(this, EmailVerificationActivity::class.java)
                        i.putExtra("registered_email", email)
                        i.putExtra("registered_name", fullName)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(i)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Account created! Please verify your email.",
                            Toast.LENGTH_LONG
                        ).show()
                        val i = Intent(this, EmailVerificationActivity::class.java)
                        i.putExtra("registered_email", email)
                        i.putExtra("registered_name", fullName)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(i)
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                btnRegister!!.isEnabled = true
                btnRegister!!.text = "CREATE ACCOUNT"

                val msg = e.message
                val message: String
                if (msg != null && msg.contains("email address is already in use")) {
                    message = "This email is already registered. Try logging in."
                } else if (msg != null && msg.contains("badly formatted")) {
                    message = "Invalid email format."
                } else if (msg != null && msg.contains("network")) {
                    message = "Network error. Check your connection."
                } else {
                    message = "Registration failed. Please try again."
                }
                tilEmail!!.error = message
            }
    }

    // ── Style "Log In" red ───────────────────────────────────────
    private fun styleLoginText() {
        val fullText = "Already have an account? Log In"
        val spannable = SpannableString(fullText)
        val redColor = ContextCompat.getColor(this, R.color.jt_red)
        val start = fullText.indexOf("Log In")
        spannable.setSpan(
            ForegroundColorSpan(redColor),
            start,
            start + "Log In".length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvLogin!!.text = spannable
    }
}