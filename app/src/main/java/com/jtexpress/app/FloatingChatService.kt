package com.jtexpress.app

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.abs

class FloatingChatService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    // Track drag vs tap
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the floating bubble layout
        floatingView = LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_bubble, null)

        // WindowManager layout params — overlay type works on all Android versions
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200  // start position — 200px from top
        }

        windowManager.addView(floatingView, params)
        setupTouchListener()
        setupPulseAnimation()
    }

    private fun setupTouchListener() {
        val bubbleRoot = floatingView.findViewById<View>(R.id.bubble_root)

        bubbleRoot.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX      = params.x
                    initialY      = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging    = false

                    // Scale down slightly on press
                    bubbleRoot.animate()
                        .scaleX(0.9f).scaleY(0.9f)
                        .setDuration(100).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    // Threshold to distinguish tap from drag
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        bubbleRoot.animate()
                            .scaleX(1.05f).scaleY(1.05f)
                            .setDuration(80).start()
                    }

                    if (isDragging) {
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    bubbleRoot.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(100).start()

                    if (!isDragging) {
                        // TAP → open chat
                        openChatActivity()
                    } else {
                        // DRAG END → snap to nearest edge
                        snapToEdge()
                    }
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Snaps the bubble to the left or right edge of the screen,
     * just like Facebook Messenger chat heads.
     */
    private fun snapToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth    = displayMetrics.widthPixels
        val bubbleWidth    = floatingView.width.takeIf { it > 0 } ?: 180

        val snapX = if (params.x + bubbleWidth / 2 < screenWidth / 2) {
            16  // snap to left edge with small margin
        } else {
            screenWidth - bubbleWidth - 16  // snap to right edge
        }

        // Smooth animated snap
        val startX = params.x
        val animator = android.animation.ValueAnimator.ofInt(startX, snapX)
        animator.duration = 250
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            params.x = anim.animatedValue as Int
            try { windowManager.updateViewLayout(floatingView, params) } catch (e: Exception) { /* view removed */ }
        }
        animator.start()
    }

    /**
     * Subtle pulse animation on the bubble so users notice it.
     */
    private fun setupPulseAnimation() {
        val bubbleRoot = floatingView.findViewById<View>(R.id.bubble_root)
        val pulse = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            bubbleRoot,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.08f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.08f, 1f)
        ).apply {
            duration     = 1800
            repeatCount  = android.animation.ObjectAnimator.INFINITE
            repeatMode   = android.animation.ObjectAnimator.RESTART
            startDelay   = 2000  // wait 2s before starting pulse
        }
        pulse.start()
    }

    private fun openChatActivity() {
        val intent = Intent(this, AiChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            try { windowManager.removeView(floatingView) } catch (e: Exception) { /* already removed */ }
        }
    }

    companion object {
        /** Call this from MainActivity/HomeFragment to show the bubble */
        fun start(context: android.content.Context) {
            context.startService(Intent(context, FloatingChatService::class.java))
        }

        /** Call this to hide the bubble (e.g. when user logs out) */
        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, FloatingChatService::class.java))
        }
    }
}