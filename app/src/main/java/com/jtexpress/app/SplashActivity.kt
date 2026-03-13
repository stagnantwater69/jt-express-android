package com.jtexpress.app

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


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY = 3000L // 3 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animate the center logo group
        val centerGroup = findViewById<LinearLayout>(R.id.center_group)
        animateLogo(centerGroup)

        // Navigate to LoginActivity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            goToLogin()
        }, SPLASH_DELAY)
    }

    private fun animateLogo(view: android.view.View) {
        // Scale animation: grow from 0.5x to 1x
        val scaleAnim = ScaleAnimation(
            0.5f, 1.0f,   // X: from 0.5 to 1.0
            0.5f, 1.0f,   // Y: from 0.5 to 1.0
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            fillAfter = true
        }

        // Fade in animation
        val fadeAnim = AlphaAnimation(0f, 1f).apply {
            duration = 800
            fillAfter = true
        }

        // Combine animations
        val animSet = AnimationSet(true).apply {
            addAnimation(scaleAnim)
            addAnimation(fadeAnim)
        }

        view.startAnimation(animSet)
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        // Smooth slide transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // Optional: Check if user is already logged in and skip to MainActivity
    private fun checkLoginState(): Boolean {
        val sharedPref = getSharedPreferences("JTExpressPrefs", MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }
}