package com.jtexpress.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class MeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_me, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfile(view)
        setupMenuItems(view)
        setupLogout(view)
    }

    // ── Load user data from SharedPreferences ───────────────────
    private fun loadProfile(view: View) {
        val prefs = requireActivity()
            .getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)

        val name  = prefs.getString("user_name", "Juan Dela Cruz") ?: "Juan Dela Cruz"
        val email = prefs.getString("user_email", "juan@email.com") ?: "juan@email.com"

        view.findViewById<TextView>(R.id.tv_profile_name).text  = name
        // FIX: correctly shows email (not phone number) from SharedPreferences
        view.findViewById<TextView>(R.id.tv_profile_email).text = email

        // Build initials from name (up to 2 words)
        val initials = name.trim()
            .split("\\s+".toRegex())
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
        view.findViewById<TextView>(R.id.tv_avatar_large).text = initials.ifEmpty { "JD" }
    }

    // ── Menu click listeners ─────────────────────────────────────
    private fun setupMenuItems(view: View) {

        // Stats row
        view.findViewById<LinearLayout>(R.id.btn_carrots).setOnClickListener {
            Toast.makeText(requireContext(), "My Carrots — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.btn_vouchers).setOnClickListener {
            Toast.makeText(requireContext(), "My Vouchers — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.btn_my_orders_stat).setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }

        // FIX: settings button is now a FrameLayout, not an ImageButton — use View
        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            Toast.makeText(requireContext(), "Settings — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // FIX: edit avatar is a FrameLayout
        view.findViewById<FrameLayout>(R.id.btn_edit_avatar).setOnClickListener {
            Toast.makeText(requireContext(), "Change profile photo — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Account section
        view.findViewById<LinearLayout>(R.id.menu_edit_profile).setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.menu_address_book).setOnClickListener {
            Toast.makeText(requireContext(), "Address Book — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.menu_change_password).setOnClickListener {
            Toast.makeText(requireContext(), "Change Password — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Support section
        view.findViewById<LinearLayout>(R.id.menu_contact).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Contact J&T Express")
                .setItems(arrayOf("📞  Call Hotline", "📧  Send Email")) { _, which ->
                    when (which) {
                        0 -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+63288911818")))
                        1 -> startActivity(Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@jtexpress.ph")
                        })
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        view.findViewById<LinearLayout>(R.id.menu_faq).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.jtexpress.ph/faq")))
        }
        view.findViewById<LinearLayout>(R.id.menu_rate_app).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.jtexpress.app")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.jtexpress.app")))
            }
        }

        // General section
        view.findViewById<LinearLayout>(R.id.menu_terms).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.jtexpress.ph/terms")))
        }
        view.findViewById<LinearLayout>(R.id.menu_privacy).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.jtexpress.ph/privacy")))
        }
        view.findViewById<LinearLayout>(R.id.menu_clear_cache).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear Cache")
                .setMessage("Are you sure you want to clear the app cache?")
                .setPositiveButton("Clear") { _, _ ->
                    requireContext().cacheDir.deleteRecursively()
                    Toast.makeText(requireContext(), "Cache cleared ✓", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ── Logout ───────────────────────────────────────────────────
    private fun setupLogout(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    requireActivity()
                        .getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()

                    val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}