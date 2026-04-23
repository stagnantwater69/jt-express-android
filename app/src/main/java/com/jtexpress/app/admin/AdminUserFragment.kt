package com.jtexpress.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class AdminUserFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_users, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUsers(view)

        // Role filter chips
        listOf(
            R.id.chip_role_all,
            R.id.chip_role_customer,
            R.id.chip_role_rider,
            R.id.chip_role_staff,
            R.id.chip_role_admin
        ).forEach { id ->
            view.findViewById<TextView>(id)?.setOnClickListener {
                val roleFilter = when (id) {
                    R.id.chip_role_customer -> "customer"
                    R.id.chip_role_rider    -> "rider"
                    R.id.chip_role_staff    -> "staff"
                    R.id.chip_role_admin    -> "admin"
                    else                    -> "all"
                }
                loadUsers(view, roleFilter)
            }
        }
    }

    private fun loadUsers(view: View, roleFilter: String = "all") {
        val container = view.findViewById<LinearLayout>(R.id.users_list_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_no_users)
        container?.removeAllViews()

        val query = if (roleFilter == "all") db.collection("users").get()
                    else db.collection("users").whereEqualTo("role", roleFilter).get()

        query.addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            if (snapshot.isEmpty) {
                tvEmpty?.visibility = View.VISIBLE
                return@addOnSuccessListener
            }
            tvEmpty?.visibility = View.GONE

            snapshot.documents.forEach { doc ->
                val name     = "${doc.getString("firstName")} ${doc.getString("lastName")}"
                val email    = doc.getString("email") ?: ""
                val role     = doc.getString("role") ?: "customer"
                val isActive = doc.getBoolean("isActive") ?: true
                val dp       = resources.displayMetrics.density

                val card = CardView(requireContext()).apply {
                    radius        = 10f * dp
                    cardElevation = 2f * dp
                    setCardBackgroundColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, (8 * dp).toInt()) }
                }

                val inner = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val p = (14 * dp).toInt()
                    setPadding(p, p, p, p)
                    gravity = Gravity.CENTER_VERTICAL
                }

                val col = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                col.addView(TextView(requireContext()).apply {
                    text = name; textSize = 14f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_black))
                    setTypeface(null, Typeface.BOLD)
                })
                col.addView(TextView(requireContext()).apply {
                    text = email; textSize = 11f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_gray))
                })
                col.addView(TextView(requireContext()).apply {
                    text = role.uppercase(); textSize = 10f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_red))
                })
                inner.addView(col)

                // Toggle active/inactive
                val btnToggle = AppCompatButton(requireContext()).apply {
                    text = if (isActive) "Deactivate" else "Activate"
                    textSize = 11f; isAllCaps = false
                    setTextColor(ContextCompat.getColor(requireContext(),
                        if (isActive) R.color.jt_red else android.R.color.holo_green_dark))
                    setBackgroundResource(R.drawable.bg_btn_outline_red)
                    layoutParams = LinearLayout.LayoutParams((90 * dp).toInt(), (36 * dp).toInt())
                    setOnClickListener {
                        doc.reference.update("isActive", !isActive)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    if (isActive) "User deactivated" else "User activated",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadUsers(view, roleFilter)
                            }
                    }
                }

                inner.addView(btnToggle)
                card.addView(inner)
                container?.addView(card)
            }
        }
    }
}
