package com.jtexpress.app.shared

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.jtexpress.app.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object { private const val SPLASH_DELAY = 2500L }

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        animateLogo(findViewById(R.id.center_group))

        Handler(Looper.getMainLooper()).postDelayed({ routeUser() }, SPLASH_DELAY)
    }

    private fun routeUser() {
        val currentUser = auth.currentUser
        if (currentUser == null) { goToLogin(); return }

        currentUser.reload().addOnCompleteListener { task ->
            if (!task.isSuccessful || auth.currentUser == null) {
                auth.signOut(); goToLogin(); return@addOnCompleteListener
            }
            val freshUser = auth.currentUser!!
            if (!freshUser.isEmailVerified) {
                auth.signOut(); goToLogin()
            } else {
                // KEY CHANGE: go to RoleRouter, not MainActivity
                startActivity(Intent(this, RoleRouterActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    private fun animateLogo(view: android.view.View) {
        val scaleAnim = ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 800; fillAfter = true }

        val fadeAnim = AlphaAnimation(0f, 1f).apply { duration = 800; fillAfter = true }

        AnimationSet(true).apply {
            addAnimation(scaleAnim); addAnimation(fadeAnim)
            view.startAnimation(this)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}