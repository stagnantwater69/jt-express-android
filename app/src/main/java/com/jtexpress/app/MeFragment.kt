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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.shared.LoginActivity

class MeFragment : Fragment() {

    private var db: FirebaseFirestore? = null
    private var uid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_me, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db  = FirebaseFirestore.getInstance()
        uid = requireActivity()
            .getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
            .getString("user_uid", "") ?: ""

        loadProfile(view)
        loadStats(view)
        setupMenuItems(view)
        setupLogout(view)
    }

    // ── 1. Profile — name + email + avatar ───────────────────────
    private fun loadProfile(view: View) {
        val prefs    = requireActivity().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        val email    = prefs.getString("user_email", "") ?: ""
        val tvName   = view.findViewById<TextView>(R.id.tv_profile_name)
        val tvEmail  = view.findViewById<TextView>(R.id.tv_profile_email)
        val tvAvatar = view.findViewById<TextView>(R.id.tv_avatar_large)

        tvEmail?.text = email

        if (uid.isEmpty()) {
            val name = prefs.getString("user_name", "User") ?: "User"
            tvName?.text = name
            setAvatarInitials(tvAvatar, name)
            return
        }

        db!!.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                if (doc != null && doc.exists()) {
                    val firstName  = doc.getString("firstName") ?: ""
                    val middleName = doc.getString("middleName") ?: ""
                    val lastName   = doc.getString("lastName") ?: ""
                    val fullName   = doc.getString("fullName") ?: ""

                    // Build display name — prefer firstName+lastName,
                    // fall back to fullName field if firstName is empty
                    val displayName = when {
                        firstName.isNotEmpty() && middleName.isNotEmpty() ->
                            "$firstName ${middleName[0].uppercaseChar()}. $lastName"
                        firstName.isNotEmpty() ->
                            "$firstName $lastName"
                        fullName.isNotEmpty() ->
                            fullName
                        else -> "User"
                    }.trim()

                    tvName?.text = displayName
                    setAvatarInitials(tvAvatar, displayName)
                    prefs.edit().putString("user_name", displayName).apply()

                } else {
                    val name = prefs.getString("user_name", "User") ?: "User"
                    tvName?.text = name
                    setAvatarInitials(tvAvatar, name)
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                val name = prefs.getString("user_name", "User") ?: "User"
                tvName?.text = name
                setAvatarInitials(tvAvatar, name)
            }
    }

    // ── 2. Stats — carrots, vouchers, orders count ───────────────
    private fun loadStats(view: View) {
        val tvCarrots  = view.findViewById<TextView>(R.id.tv_carrots)
        val tvVouchers = view.findViewById<TextView>(R.id.tv_vouchers)
        val tvOrders   = view.findViewById<TextView>(R.id.tv_orders_count)

        if (uid.isEmpty()) return

        // ── Carrots + Vouchers from users/{uid} ───────────────────
        db!!.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                tvCarrots?.text  = (doc?.getLong("carrots")  ?: 0L).toString()
                tvVouchers?.text = (doc?.getLong("vouchers") ?: 0L).toString()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                tvCarrots?.text  = "0"
                tvVouchers?.text = "0"
            }

        // ── Orders count from orders collection ───────────────────
        db!!.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                tvOrders?.text = (snapshot?.size() ?: 0).toString()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                tvOrders?.text = "0"
            }
    }

    // ── Avatar initials ───────────────────────────────────────────
    private fun setAvatarInitials(tvAvatar: TextView?, name: String) {
        if (tvAvatar == null) return
        val parts    = name.trim().split(" ")
        val initials = StringBuilder()
        var i        = 0
        while (i < parts.size && i < 2) {
            if (parts[i].isNotEmpty() && parts[i][0].isLetter()) {
                initials.append(parts[i][0].uppercaseChar())
            }
            i++
        }
        tvAvatar.text = if (initials.isEmpty()) "??" else initials.toString()
    }

    // ── Menu click listeners ──────────────────────────────────────
    private fun setupMenuItems(view: View) {

        view.findViewById<LinearLayout>(R.id.btn_carrots)?.setOnClickListener {
            Toast.makeText(requireContext(), "My Carrots — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.btn_vouchers)?.setOnClickListener {
            Toast.makeText(requireContext(), "My Vouchers — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.btn_my_orders_stat)?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }
        view.findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            Toast.makeText(requireContext(), "Settings — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_edit_avatar)?.setOnClickListener {
            Toast.makeText(requireContext(), "Change profile photo — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Account
        view.findViewById<LinearLayout>(R.id.menu_edit_profile)?.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.menu_address_book)?.setOnClickListener {
            Toast.makeText(requireContext(), "Address Book — Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<LinearLayout>(R.id.menu_change_password)?.setOnClickListener {
            Toast.makeText(requireContext(), "Change Password — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Support
        view.findViewById<LinearLayout>(R.id.menu_contact)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Contact J&T Express")
                .setItems(arrayOf("Call Hotline", "Send Email")) { _, which ->
                    if (which == 0) {
                        // Pull hotline dynamically from Firestore
                        db!!.collection("app_config").document("contact").get()
                            .addOnSuccessListener { doc ->
                                if (!isAdded) return@addOnSuccessListener
                                val number = doc?.getString("hotline") ?: "+63288911818"
                                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                            }
                            .addOnFailureListener {
                                startActivity(Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:+63288911818")))
                            }
                    } else {
                        startActivity(Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@jtexpress.ph")
                        })
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<LinearLayout>(R.id.menu_faq)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.jtexpress.ph/faq")))
        }

        view.findViewById<LinearLayout>(R.id.menu_rate_app)?.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.jtexpress.app")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.jtexpress.app")))
            }
        }

        // General
        view.findViewById<LinearLayout>(R.id.menu_terms)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.jtexpress.ph/terms")))
        }
        view.findViewById<LinearLayout>(R.id.menu_privacy)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.jtexpress.ph/privacy")))
        }
        view.findViewById<LinearLayout>(R.id.menu_clear_cache)?.setOnClickListener {
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

    // ── Logout ────────────────────────────────────────────────────
    private fun setupLogout(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_logout)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ -> performLogout() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performLogout() {
        val ctx = requireContext()
        FloatingChatService.stop(ctx)
        requireActivity()
            .getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(ctx, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }
}