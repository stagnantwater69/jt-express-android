package com.jtexpress.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * OverlayPermissionActivity
 *
 * Android requires special permission (SYSTEM_ALERT_WINDOW) to draw
 * windows over other apps — the same permission Facebook Messenger uses.
 *
 * This activity asks the user for that permission, then starts the
 * FloatingChatService once granted.
 *
 * Usage: call OverlayPermissionActivity.requestIfNeeded(context) from
 * MainActivity after login.
 */
class OverlayPermissionActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_OVERLAY = 1001

        /**
         * Call this from MainActivity / HomeFragment.
         * It checks if permission is already granted before doing anything.
         */
        fun requestIfNeeded(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context)
            ) {
                // Need to ask — open this activity
                val intent = Intent(context, OverlayPermissionActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                // Already have permission — start the bubble directly
                FloatingChatService.start(context)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                // Permission already granted
                FloatingChatService.start(this)
                finish()
                return
            }

            // Show a friendly explanation before sending to Settings
            AlertDialog.Builder(this)
                .setTitle("Enable AI Assistant Bubble")
                .setMessage(
                    "J&T Express would like to show a floating AI chat bubble " +
                            "so you can access your assistant from anywhere in the app — " +
                            "just like Messenger chat heads.\n\n" +
                            "Tap 'Allow' and enable the permission on the next screen."
                )
                .setPositiveButton("Allow") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY)
                }
                .setNegativeButton("Not Now") { _, _ ->
                    Toast.makeText(this, "You can enable the bubble later in Settings.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setCancelable(false)
                .show()
        } else {
            // Below Android 6 — permission automatic
            FloatingChatService.start(this)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Settings.canDrawOverlays(this)
            ) {
                // User granted it
                FloatingChatService.start(this)
                Toast.makeText(this, "AI Assistant bubble enabled! ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied. Bubble won't show.", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}