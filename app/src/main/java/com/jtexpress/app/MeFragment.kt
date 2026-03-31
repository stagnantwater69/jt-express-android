package com.jtexpress.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

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

    // ── Load user data from Firestore + SharedPreferences ────────
    private fun loadProfile(view: View) {
        val prefs = requireActivity()
            .getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)

        val uid   = prefs.getString("user_uid", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""

        val tvName   = view.findViewById<TextView>(R.id.tv_profile_name)
        val tvEmail  = view.findViewById<TextView>(R.id.tv_profile_email)
        val tvAvatar = view.findViewById<TextView>(R.id.tv_avatar_large)

        // Set email immediately from prefs
        if (tvEmail != null) tvEmail.text = email

        if (uid.isEmpty()) {
            // No UID — fallback to prefs name
            val name = prefs.getString("user_name", "User") ?: "User"
            if (tvName != null) tvName.text = name
            setAvatarInitials(tvAvatar, name)
            return
        }

        // Fetch fresh data from Firestore
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val firstName  = doc.getString("firstName") ?: ""
                    val middleName = doc.getString("middleName") ?: ""
                    val lastName   = doc.getString("lastName") ?: ""

                    // ✅ Format middle name as single initial if provided
                    val displayName: String
                    if (middleName.isEmpty()) {
                        displayName = "$firstName $lastName"
                    } else {
                        // Take only first letter of middle name → uppercase + "."
                        val middleInitial = middleName[0].uppercaseChar()
                        displayName = "$firstName $middleInitial. $lastName"
                    }

                    val trimmedName = trimName(displayName)

                    if (tvName != null) tvName.text = trimmedName
                    setAvatarInitials(tvAvatar, trimmedName)

                    // Update SharedPrefs with latest name
                    val editor = prefs.edit()
                    editor.putString("user_name", trimmedName)
                    editor.apply()

                } else {
                    // No doc — fallback to prefs
                    val name = prefs.getString("user_name", "User") ?: "User"
                    if (tvName != null) tvName.text = name
                    setAvatarInitials(tvAvatar, name)
                }
            }
            .addOnFailureListener {
                // Firestore failed — fallback to prefs
                val name = prefs.getString("user_name", "User") ?: "User"
                if (tvName != null) tvName.text = name
                setAvatarInitials(tvAvatar, name)
            }
    }

    // ── Build avatar initials from display name ──────────────────
    private fun setAvatarInitials(tvAvatar: TextView?, name: String) {
        if (tvAvatar == null) return
        val parts = name.split(" ")
        val initials = StringBuilder()
        var i = 0
        while (i < parts.size && i < 2) {
            if (parts[i].isNotEmpty() && parts[i][0].isLetter()) {
                initials.append(parts[i][0].uppercaseChar())
            }
            i++
        }
        tvAvatar.text = if (initials.isEmpty()) "??" else initials.toString()
    }

    // ── Manual trim ──────────────────────────────────────────────
    private fun trimName(input: String): String {
        var start = 0
        var end = input.length
        while (start < end && input[start] == ' ') start++
        while (end > start && input[end - 1] == ' ') end--
        return input.substring(start, end)
    }

    // ── Menu click listeners ─────────────────────────────────────
    private fun setupMenuItems(view: View) {

        // Stats row
        val btnCarrots = view.findViewById<LinearLayout>(R.id.btn_carrots)
        val btnVouchers = view.findViewById<LinearLayout>(R.id.btn_vouchers)
        val btnOrdersStat = view.findViewById<LinearLayout>(R.id.btn_my_orders_stat)
        val btnSettings = view.findViewById<View>(R.id.btn_settings)
        val btnEditAvatar = view.findViewById<View>(R.id.btn_edit_avatar)

        if (btnCarrots != null) {
            btnCarrots.setOnClickListener {
                Toast.makeText(requireContext(), "My Carrots — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }
        if (btnVouchers != null) {
            btnVouchers.setOnClickListener {
                Toast.makeText(requireContext(), "My Vouchers — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }
        if (btnOrdersStat != null) {
            btnOrdersStat.setOnClickListener {
                (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
            }
        }
        if (btnSettings != null) {
            btnSettings.setOnClickListener {
                Toast.makeText(requireContext(), "Settings — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }
        if (btnEditAvatar != null) {
            btnEditAvatar.setOnClickListener {
                Toast.makeText(requireContext(), "Change profile photo — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }

        // Account section
        val menuEditProfile = view.findViewById<LinearLayout>(R.id.menu_edit_profile)
        val menuAddressBook = view.findViewById<LinearLayout>(R.id.menu_address_book)
        val menuChangePassword = view.findViewById<LinearLayout>(R.id.menu_change_password)

        if (menuEditProfile != null) {
            menuEditProfile.setOnClickListener {
                Toast.makeText(requireContext(), "Edit Profile — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }
        if (menuAddressBook != null) {
            menuAddressBook.setOnClickListener {
                Toast.makeText(requireContext(), "Address Book — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }
        if (menuChangePassword != null) {
            menuChangePassword.setOnClickListener {
                Toast.makeText(requireContext(), "Change Password — Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }

        // Support section
        val menuContact = view.findViewById<LinearLayout>(R.id.menu_contact)
        val menuFaq = view.findViewById<LinearLayout>(R.id.menu_faq)
        val menuRateApp = view.findViewById<LinearLayout>(R.id.menu_rate_app)

        if (menuContact != null) {
            menuContact.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Contact J&T Express")
                    .setItems(arrayOf("Call Hotline", "Send Email")) { _, which ->
                        if (which == 0) {
                            startActivity(Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:+63288911818")))
                        } else {
                            val emailIntent = Intent(Intent.ACTION_SENDTO)
                            emailIntent.data = Uri.parse("mailto:support@jtexpress.ph")
                            startActivity(emailIntent)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        if (menuFaq != null) {
            menuFaq.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.jtexpress.ph/faq")))
            }
        }
        if (menuRateApp != null) {
            menuRateApp.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.jtexpress.app")))
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.jtexpress.app")))
                }
            }
        }

        // General section
        val menuTerms = view.findViewById<LinearLayout>(R.id.menu_terms)
        val menuPrivacy = view.findViewById<LinearLayout>(R.id.menu_privacy)
        val menuClearCache = view.findViewById<LinearLayout>(R.id.menu_clear_cache)

        if (menuTerms != null) {
            menuTerms.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.jtexpress.ph/terms")))
            }
        }
        if (menuPrivacy != null) {
            menuPrivacy.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.jtexpress.ph/privacy")))
            }
        }
        if (menuClearCache != null) {
            menuClearCache.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Clear Cache")
                    .setMessage("Are you sure you want to clear the app cache?")
                    .setPositiveButton("Clear") { _, _ ->
                        requireContext().cacheDir.deleteRecursively()
                        Toast.makeText(requireContext(), "Cache cleared!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    // ── Logout — delegates to MainActivity ──────────────────────
    private fun setupLogout(view: View) {
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)
        if (btnLogout != null) {
            btnLogout.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out") { _, _ ->
                        // ✅ Delegate logout to MainActivity
                        val mainActivity = activity as? MainActivity
                        if (mainActivity != null) {
                            mainActivity.logout()
                        } else {
                            // Fallback if cast fails
                            requireActivity().finishAffinity()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}