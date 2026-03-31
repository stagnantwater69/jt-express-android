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
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY = 3000L
    }

    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        val centerGroup = findViewById<LinearLayout>(R.id.center_group)
        animateLogo(centerGroup)

        Handler(Looper.getMainLooper()).postDelayed({
            routeUser()
        }, SPLASH_DELAY)
    }

    private fun routeUser() {
        val currentUser = auth!!.currentUser

        if (currentUser == null) {
            // ❌ Not logged in → go to Login
            goToLogin()
            return
        }

        // Reload user to get fresh status
        currentUser.reload().addOnCompleteListener { task ->
            if (!task.isSuccessful || auth!!.currentUser == null) {
                // Session expired or reload failed → go to Login
                auth!!.signOut()
                goToLogin()
                return@addOnCompleteListener
            }

            val freshUser = auth!!.currentUser!!

            if (!freshUser.isEmailVerified) {
                // ⚠️ Not verified → sign out and go to Login
                auth!!.signOut()
                goToLogin()
            } else {
                // ✅ Logged in and verified → go to Main
                val i = Intent(this, MainActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    private fun animateLogo(view: android.view.View) {
        val scaleAnim = ScaleAnimation(
            0.5f, 1.0f,
            0.5f, 1.0f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnim.duration = 800
        scaleAnim.fillAfter = true

        val fadeAnim = AlphaAnimation(0f, 1f)
        fadeAnim.duration = 800
        fadeAnim.fillAfter = true

        val animSet = AnimationSet(true)
        animSet.addAnimation(scaleAnim)
        animSet.addAnimation(fadeAnim)

        view.startAnimation(animSet)
    }

    private fun goToLogin() {
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}