package com.jtexpress.app.shared

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.jtexpress.app.R

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Step cards
    private lateinit var cardStep1: CardView
    private lateinit var cardStep2: CardView

    // Step indicators (only 2 now)
    private lateinit var step1Dot: TextView
    private lateinit var step2Dot: TextView

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
    private lateinit var btnOpenEmail: MaterialButton

    private var currentStep = 1
    private var userEmail   = ""
    private var resendCooldownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()
        styleBottomHint()

        val prefillEmail = intent.getStringExtra("prefill_email")
        if (!prefillEmail.isNullOrEmpty()) {
            etEmail.setText(prefillEmail)
        }
    }

    private fun initViews() {
        // ── Cards ─────────────────────────────────────────────────
        cardStep1 = findViewById(R.id.card_step1)
        cardStep2 = findViewById(R.id.card_step2)

        // card_step3 exists in XML as visibility=gone — no reference needed in code
        // step3_dot, et_otp1..6 are REMOVED from XML — do NOT reference them here

        // ── Step indicators ───────────────────────────────────────
        step1Dot = findViewById(R.id.step1_dot)
        step2Dot = findViewById(R.id.step2_dot)

        // ── Header ────────────────────────────────────────────────
        tvTitle      = findViewById(R.id.tv_title)
        tvSubtitle   = findViewById(R.id.tv_subtitle)
        btnBack      = findViewById(R.id.btn_back)
        tvBottomHint = findViewById(R.id.tv_bottom_hint)

        // ── Step 1 ────────────────────────────────────────────────
        tilEmail       = findViewById(R.id.til_email)
        etEmail        = findViewById(R.id.et_email)
        btnSendCode    = findViewById(R.id.btn_send_code)
        btnBackToLogin = findViewById(R.id.btn_back_to_login)

        // ── Step 2 ────────────────────────────────────────────────
        tvEmailSentTo = findViewById(R.id.tv_email_sent_to)
        tvTimer       = findViewById(R.id.tv_timer)
        tvResend      = findViewById(R.id.tv_resend)
        btnOpenEmail  = findViewById(R.id.btn_verify_code) // reused id from old layout
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            if (currentStep > 1) goToStep(1) else finish()
        }
        btnBackToLogin.setOnClickListener { finish() }
        tvBottomHint.setOnClickListener  { finish() }

        // Step 1 — send Firebase reset link
        btnSendCode.setOnClickListener {
            if (validateEmail()) sendPasswordResetEmail()
        }

        // Step 2 — open device email app
        btnOpenEmail.setOnClickListener { openEmailApp() }

        // Step 2 — resend with cooldown
        tvResend.setOnClickListener {
            if (resendCooldownTimer == null) {
                sendPasswordResetEmail(isResend = true)
            } else {
                Toast.makeText(this, "Please wait before resending.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Validate email ────────────────────────────────────────────

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        tilEmail.error = null
        return when {
            email.isEmpty() -> {
                tilEmail.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Enter a valid email address"
                false
            }
            else -> true
        }
    }

    // ── Send Firebase password reset email ───────────────────────

    private fun sendPasswordResetEmail(isResend: Boolean = false) {
        userEmail = etEmail.text.toString().trim()

        if (!isResend) {
            btnSendCode.isEnabled = false
            btnSendCode.text      = "Sending…"
        }

        auth.sendPasswordResetEmail(userEmail)
            .addOnSuccessListener {
                if (isResend) {
                    Toast.makeText(this, "Reset link resent to $userEmail", Toast.LENGTH_SHORT).show()
                    startResendCooldown()
                } else {
                    goToStep(2)
                }
            }
            .addOnFailureListener { e ->
                btnSendCode.isEnabled = true
                btnSendCode.text      = "SEND RESET LINK"

                val msg = e.message ?: ""
                tilEmail.error = when {
                    msg.contains("no user record", ignoreCase = true) ||
                            msg.contains("user-not-found", ignoreCase = true) ->
                        "No account found with this email."
                    msg.contains("network", ignoreCase = true) ->
                        "Network error. Check your connection."
                    msg.contains("too-many-requests", ignoreCase = true) ->
                        "Too many attempts. Try again later."
                    else ->
                        "Failed to send reset link. Try again."
                }
            }
    }

    // ── Open device email app ─────────────────────────────────────

    private fun openEmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No email app found. Please open your email manually.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ── Resend cooldown ───────────────────────────────────────────

    private fun startResendCooldown() {
        tvResend.isClickable = false
        tvResend.alpha       = 0.5f

        resendCooldownTimer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(ms: Long) {
                tvResend.text = "Resend available in ${ms / 1000}s"
            }
            override fun onFinish() {
                resendCooldownTimer = null
                tvResend.text        = "Didn't receive it? Resend"
                tvResend.isClickable = true
                tvResend.alpha       = 1.0f
            }
        }.start()
    }

    // ── Step navigation ───────────────────────────────────────────

    private fun goToStep(step: Int) {
        currentStep = step

        cardStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        cardStep2.visibility = if (step == 2) View.VISIBLE else View.GONE

        updateStepIndicators(step)

        when (step) {
            1 -> {
                tvTitle.text          = "Forgot Password?"
                tvSubtitle.text       = "We'll send a reset link to your email"
                btnSendCode.isEnabled = true
                btnSendCode.text      = "SEND RESET LINK"
            }
            2 -> {
                tvTitle.text    = "Check Your Email"
                tvSubtitle.text = "Reset link sent successfully"

                tvEmailSentTo.text =
                    "We sent a password reset link to:\n\n$userEmail\n\n" +
                            "Open your email and tap the link to set a new password.\n\n" +
                            "✅ After resetting, come back and log in with your new password."

                tvTimer.text       = "⏱ The link expires in 10 minutes."
                tvTimer.visibility = View.VISIBLE

                btnOpenEmail.text      = "OPEN EMAIL APP"
                btnOpenEmail.isEnabled = true

                startResendCooldown()
            }
        }
    }

    private fun updateStepIndicators(step: Int) {
        val white = ContextCompat.getColor(this, R.color.jt_white)
        val red   = ContextCompat.getColor(this, R.color.jt_red)

        listOf(step1Dot, step2Dot).forEachIndexed { index, dot ->
            val isActive = index + 1 == step
            val isDone   = index + 1 < step
            dot.alpha      = if (isActive || isDone) 1.0f else 0.5f
            dot.setTextColor(if (isActive) red else white)
            dot.background = ContextCompat.getDrawable(
                this,
                if (isActive) R.drawable.circle_white_bg else R.drawable.circle_outline_white
            )
            dot.text = if (isDone) "✓" else "${index + 1}"
        }
    }

    private fun styleBottomHint() {
        val fullText  = "Remembered your password? Log In"
        val spannable = SpannableString(fullText)
        val redColor  = ContextCompat.getColor(this, R.color.jt_red)
        val start     = fullText.indexOf("Log In")
        spannable.setSpan(
            ForegroundColorSpan(redColor), start, start + 6,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvBottomHint.text = spannable
    }

    override fun onDestroy() {
        super.onDestroy()
        resendCooldownTimer?.cancel()
    }
}