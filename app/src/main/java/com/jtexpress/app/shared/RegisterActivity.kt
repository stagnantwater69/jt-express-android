package com.jtexpress.app.shared

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
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
import com.jtexpress.app.R

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

    // Role selection
    private var selectedRole: String = "customer"
    private val roleOptions = listOf("customer", "rider", "staff", "admin")
    private val roleLabels  = listOf("Customer", "Rider", "Staff", "Admin")
    private val roleViews   = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        initViews()
        setupRoleSelector()
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

    // ── Role Selector ─────────────────────────────────────────────
    private fun setupRoleSelector() {
        val container = findViewById<LinearLayout>(R.id.ll_role_selector) ?: return
        val dm        = resources.displayMetrics
        val dp        = dm.density

        roleViews.clear()
        roleOptions.forEachIndexed { index, role ->
            val tv = TextView(this).apply {
                text      = roleLabels[index]
                textSize  = 12f
                isAllCaps = false
                gravity   = android.view.Gravity.CENTER
                setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { setMargins((2 * dp).toInt(), 0, (2 * dp).toInt(), 0) }
                setBackgroundResource(
                    if (role == selectedRole) R.drawable.chip_active_bg
                    else R.drawable.chip_inactive_bg
                )
                setTextColor(
                    if (role == selectedRole) ContextCompat.getColor(context, R.color.jt_white)
                    else ContextCompat.getColor(context, R.color.jt_gray)
                )
                setOnClickListener { selectRole(role) }
            }
            roleViews.add(tv)
            container.addView(tv)
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        roleViews.forEachIndexed { index, tv ->
            val isSelected = roleOptions[index] == role
            tv.setBackgroundResource(
                if (isSelected) R.drawable.chip_active_bg else R.drawable.chip_inactive_bg
            )
            tv.setTextColor(
                if (isSelected) ContextCompat.getColor(this, R.color.jt_white)
                else ContextCompat.getColor(this, R.color.jt_gray)
            )
        }
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

        if (firstName.isEmpty()) {
            tilFirstName!!.error = "First name is required"; isValid = false
        } else if (firstName.length < 2) {
            tilFirstName!!.error = "Enter a valid first name"; isValid = false
        }

        if (middleName.isNotEmpty() && middleName.length < 2) {
            tilMiddleName!!.error = "Enter a valid middle name"; isValid = false
        }

        if (lastName.isEmpty()) {
            tilLastName!!.error = "Last name is required"; isValid = false
        } else if (lastName.length < 2) {
            tilLastName!!.error = "Enter a valid last name"; isValid = false
        }

        if (email.isEmpty()) {
            tilEmail!!.error = "Email is required"; isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail!!.error = "Enter a valid email address"; isValid = false
        }

        if (phone.isEmpty()) {
            tilPhone!!.error = "Phone number is required"; isValid = false
        } else if (phone.length < 10) {
            tilPhone!!.error = "Enter a valid phone number (min 10 digits)"; isValid = false
        }

        if (password.isEmpty()) {
            tilPassword!!.error = "Password is required"; isValid = false
        } else if (password.length < 6) {
            tilPassword!!.error = "Password must be at least 6 characters"; isValid = false
        }

        if (confirmPass.isEmpty()) {
            tilConfirmPassword!!.error = "Please confirm your password"; isValid = false
        } else if (password != confirmPass) {
            tilConfirmPassword!!.error = "Passwords do not match"; isValid = false
        }

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

        val fullName = if (middleName.isEmpty()) "$firstName $lastName"
        else "$firstName $middleName $lastName"

        btnRegister!!.isEnabled = false
        btnRegister!!.text = "Creating account..."

        auth!!.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid

                result.user!!.sendEmailVerification()
                    .addOnFailureListener { e ->
                        android.util.Log.e("AUTH", "Failed to send verification: ${e.message}")
                    }

                // Riders and staff/admin don't need email verification flow
                // but we still send the email for consistency
                val needsVerification = selectedRole == "customer"

                val userDoc = hashMapOf<String, Any>(
                    "uid"        to uid,
                    "firstName"  to firstName,
                    "middleName" to middleName,
                    "lastName"   to lastName,
                    "fullName"   to fullName,
                    "email"      to email,
                    "phone"      to phone,
                    "role"       to selectedRole,
                    "carrots"    to 0,
                    "vouchers"   to 0,
                    // Riders/staff start as active; customers are always active
                    "isActive"   to true,
                    "createdAt"  to Timestamp.now()
                )

                db!!.collection("users").document(uid)
                    .set(userDoc)
                    .addOnSuccessListener {
                        if (needsVerification) {
                            // Customer → email verification flow
                            val i = Intent(this, EmailVerificationActivity::class.java)
                            i.putExtra("registered_email", email)
                            i.putExtra("registered_name", fullName)
                            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(i)
                        } else {
                            // Rider / Staff / Admin → go straight to login
                            // (Admin may want to manually activate staff/rider accounts)
                            Toast.makeText(
                                this,
                                "Account created as ${selectedRole.replaceFirstChar { it.uppercase() }}! Please log in.",
                                Toast.LENGTH_LONG
                            ).show()
                            val i = Intent(this, LoginActivity::class.java)
                            i.putExtra("registered_email", email)
                            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(i)
                        }
                        finish()
                    }
                    .addOnFailureListener {
                        // Firestore write failed but auth succeeded — still proceed
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

                val msg = e.message ?: ""
                val message = when {
                    msg.contains("email address is already in use") ->
                        "This email is already registered. Try logging in."
                    msg.contains("badly formatted") -> "Invalid email format."
                    msg.contains("network")         -> "Network error. Check your connection."
                    else                            -> "Registration failed. Please try again."
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
            start, start + "Log In".length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvLogin!!.text = spannable
    }
}