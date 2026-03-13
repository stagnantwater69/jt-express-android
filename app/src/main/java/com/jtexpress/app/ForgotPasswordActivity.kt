package com.jtexpress.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    // Step cards
    private lateinit var cardStep1: CardView
    private lateinit var cardStep2: CardView
    private lateinit var cardStep3: CardView

    // Step indicators
    private lateinit var step1Dot: TextView
    private lateinit var step2Dot: TextView
    private lateinit var step3Dot: TextView

    // Header
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var tvBottomHint: TextView

    // Step 1
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendCode: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton

    // Step 2
    private lateinit var tvEmailSentTo: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvResend: TextView
    private lateinit var btnVerifyCode: MaterialButton
    private lateinit var otpFields: List<EditText>
    private var countDownTimer: CountDownTimer? = null

    // Step 3
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var tvStrengthLabel: TextView
    private lateinit var tvPasswordMatch: TextView
    private lateinit var reqLength: TextView
    private lateinit var reqUppercase: TextView
    private lateinit var reqNumber: TextView
    private lateinit var strengthBars: List<View>

    private var currentStep = 1
    private var userEmail = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupClickListeners()
        setupPasswordWatcher()
        setupOtpAutoFocus()
        styleBottomHint()

        // Pre-fill email if passed from LoginActivity
        val prefillEmail = intent.getStringExtra("prefill_email")
        if (!prefillEmail.isNullOrEmpty()) {
            etEmail.setText(prefillEmail)
        }
    }

    private fun initViews() {
        cardStep1 = findViewById(R.id.card_step1)
        cardStep2 = findViewById(R.id.card_step2)
        cardStep3 = findViewById(R.id.card_step3)

        step1Dot = findViewById(R.id.step1_dot)
        step2Dot = findViewById(R.id.step2_dot)
        step3Dot = findViewById(R.id.step3_dot)

        tvTitle = findViewById(R.id.tv_title)
        tvSubtitle = findViewById(R.id.tv_subtitle)
        btnBack = findViewById(R.id.btn_back)
        tvBottomHint = findViewById(R.id.tv_bottom_hint)

        tilEmail = findViewById(R.id.til_email)
        etEmail = findViewById(R.id.et_email)
        btnSendCode = findViewById(R.id.btn_send_code)
        btnBackToLogin = findViewById(R.id.btn_back_to_login)

        tvEmailSentTo = findViewById(R.id.tv_email_sent_to)
        tvTimer = findViewById(R.id.tv_timer)
        tvResend = findViewById(R.id.tv_resend)
        btnVerifyCode = findViewById(R.id.btn_verify_code)
        otpFields = listOf(
            findViewById(R.id.et_otp1), findViewById(R.id.et_otp2),
            findViewById(R.id.et_otp3), findViewById(R.id.et_otp4),
            findViewById(R.id.et_otp5), findViewById(R.id.et_otp6)
        )

        tilNewPassword = findViewById(R.id.til_new_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnResetPassword = findViewById(R.id.btn_reset_password)
        tvStrengthLabel = findViewById(R.id.tv_strength_label)
        tvPasswordMatch = findViewById(R.id.tv_password_match)
        reqLength = findViewById(R.id.req_length)
        reqUppercase = findViewById(R.id.req_uppercase)
        reqNumber = findViewById(R.id.req_number)
        strengthBars = listOf(
            findViewById(R.id.strength_bar1), findViewById(R.id.strength_bar2),
            findViewById(R.id.strength_bar3), findViewById(R.id.strength_bar4)
        )
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            if (currentStep > 1) goToStep(currentStep - 1)
            else finish()
        }

        btnBackToLogin.setOnClickListener { finish() }
        tvBottomHint.setOnClickListener { finish() }

        btnSendCode.setOnClickListener {
            if (validateEmail()) sendVerificationCode()
        }

        btnVerifyCode.setOnClickListener {
            verifyOtpCode()
        }

        tvResend.setOnClickListener {
            countDownTimer?.cancel()
            startCountdown()
            Toast.makeText(this, "Code resent to $userEmail", Toast.LENGTH_SHORT).show()
        }

        btnResetPassword.setOnClickListener {
            if (validateNewPassword()) resetPassword()
        }
    }

    // ─── STEP 1: Validate & Send Code ───────────────────────────────────────

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        tilEmail.error = null
        return when {
            email.isEmpty() -> { tilEmail.error = "Email is required"; false }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Enter a valid email address"; false
            }
            else -> true
        }
    }

    private fun sendVerificationCode() {
        userEmail = etEmail.text.toString().trim()
        btnSendCode.isEnabled = false
        btnSendCode.text = "Sending..."

        btnSendCode.postDelayed({
            goToStep(2)
        }, 1500)
    }

    // ─── STEP 2: OTP Verification ────────────────────────────────────────────

    private fun setupOtpAutoFocus() {
        otpFields.forEachIndexed { index, field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < otpFields.size - 1) {
                        otpFields[index + 1].requestFocus()
                    } else if (s?.isNullOrEmpty() == true && index > 0) {
                        otpFields[index - 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun verifyOtpCode() {
        val code = otpFields.joinToString("") { it.text.toString() }
        if (code.length < 6) {
            Toast.makeText(this, "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }

        btnVerifyCode.isEnabled = false
        btnVerifyCode.text = "Verifying..."

        // Simulate verification (demo: any 6 digits work)
        btnVerifyCode.postDelayed({
            countDownTimer?.cancel()
            goToStep(3)
        }, 1500)
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(300000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                tvTimer.text = "Code expires in: %02d:%02d".format(minutes, seconds)
            }
            override fun onFinish() {
                tvTimer.text = "Code expired. Please request a new one."
                tvTimer.setTextColor(Color.GRAY)
            }
        }.start()
    }

    // ─── STEP 3: New Password ─────────────────────────────────────────────────

    private fun setupPasswordWatcher() {
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                updatePasswordStrength(password)
                updateRequirements(password)
                checkPasswordMatch()
            }
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { checkPasswordMatch() }
        })
    }

    private fun updatePasswordStrength(password: String) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        val colors = listOf("#E0E0E0", "#F44336", "#FF9800", "#FFC107", "#4CAF50")
        val labels = listOf("", "Weak", "Fair", "Good", "Strong")
        val textColors = listOf(Color.GRAY, Color.RED, Color.parseColor("#FF9800"),
            Color.parseColor("#FFC107"), Color.parseColor("#4CAF50"))

        strengthBars.forEachIndexed { index, bar ->
            bar.setBackgroundColor(
                if (index < score) Color.parseColor(colors[score]) else Color.parseColor("#E0E0E0")
            )
        }

        tvStrengthLabel.text = labels[score]
        tvStrengthLabel.setTextColor(textColors[score])
    }

    private fun updateRequirements(password: String) {
        val green = ContextCompat.getColor(this, R.color.jt_red)
        val gray = ContextCompat.getColor(this, R.color.jt_gray)

        reqLength.text = "${if (password.length >= 8) "✓" else "✗"}  At least 8 characters"
        reqLength.setTextColor(if (password.length >= 8) green else gray)

        reqUppercase.text = "${if (password.any { it.isUpperCase() }) "✓" else "✗"}  At least one uppercase letter"
        reqUppercase.setTextColor(if (password.any { it.isUpperCase() }) green else gray)

        reqNumber.text = "${if (password.any { it.isDigit() }) "✓" else "✗"}  At least one number"
        reqNumber.setTextColor(if (password.any { it.isDigit() }) green else gray)
    }

    private fun checkPasswordMatch() {
        val pass = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()

        when {
            confirm.isEmpty() -> { tvPasswordMatch.text = "" }
            pass == confirm -> {
                tvPasswordMatch.text = "✓ Passwords match"
                tvPasswordMatch.setTextColor(Color.parseColor("#4CAF50"))
            }
            else -> {
                tvPasswordMatch.text = "✗ Passwords do not match"
                tvPasswordMatch.setTextColor(Color.RED)
            }
        }
    }

    private fun validateNewPassword(): Boolean {
        val password = etNewPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        tilNewPassword.error = null
        tilConfirmPassword.error = null

        return when {
            password.isEmpty() -> { tilNewPassword.error = "Password is required"; false }
            password.length < 8 -> { tilNewPassword.error = "Minimum 8 characters required"; false }
            !password.any { it.isUpperCase() } -> { tilNewPassword.error = "Add at least one uppercase letter"; false }
            !password.any { it.isDigit() } -> { tilNewPassword.error = "Add at least one number"; false }
            confirm.isEmpty() -> { tilConfirmPassword.error = "Please confirm your password"; false }
            password != confirm -> { tilConfirmPassword.error = "Passwords do not match"; false }
            else -> true
        }
    }

    private fun resetPassword() {
        btnResetPassword.isEnabled = false
        btnResetPassword.text = "Resetting..."

        btnResetPassword.postDelayed({
            Toast.makeText(this, "Password reset successfully! Please log in.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("registered_email", userEmail)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 1500)
    }

    // ─── Step Navigation ──────────────────────────────────────────────────────

    private fun goToStep(step: Int) {
        currentStep = step

        cardStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        cardStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        cardStep3.visibility = if (step == 3) View.VISIBLE else View.GONE

        updateStepIndicators(step)

        when (step) {
            1 -> {
                tvTitle.text = "Forgot Password?"
                tvSubtitle.text = "We'll send a verification code to your email"
                btnSendCode.isEnabled = true
                btnSendCode.text = "SEND VERIFICATION CODE"
            }
            2 -> {
                tvTitle.text = "Enter OTP Code"
                tvSubtitle.text = "Check your inbox for the 6-digit code"
                tvEmailSentTo.text = "We sent a 6-digit code to\n$userEmail"
                otpFields.forEach { it.text?.clear() }
                otpFields[0].requestFocus()
                startCountdown()
                btnVerifyCode.isEnabled = true
                btnVerifyCode.text = "VERIFY CODE"
            }
            3 -> {
                tvTitle.text = "New Password"
                tvSubtitle.text = "Create a strong password for your account"
            }
        }
    }

    private fun updateStepIndicators(step: Int) {
        val white = ContextCompat.getColor(this, R.color.jt_white)
        val red = ContextCompat.getColor(this, R.color.jt_red)

        listOf(step1Dot, step2Dot, step3Dot).forEachIndexed { index, dot ->
            val isActive = index + 1 == step
            val isDone = index + 1 < step
            dot.alpha = if (isActive || isDone) 1.0f else 0.5f
            dot.setTextColor(if (isActive) red else white)
            dot.background = ContextCompat.getDrawable(
                this,
                if (isActive) R.drawable.circle_white_bg else R.drawable.circle_outline_white
            )
            dot.text = if (isDone) "✓" else "${index + 1}"
        }
    }

    private fun styleBottomHint() {
        val fullText = "Remembered your password? Log In"
        val spannable = SpannableString(fullText)
        val redColor = ContextCompat.getColor(this, R.color.jt_red)
        val start = fullText.indexOf("Log In")
        spannable.setSpan(ForegroundColorSpan(redColor), start, start + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvBottomHint.text = spannable
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}