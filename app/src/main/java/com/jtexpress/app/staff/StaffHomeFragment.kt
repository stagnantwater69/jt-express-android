package com.jtexpress.app.staff

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R
import com.jtexpress.app.StaffMainActivity

class StaffHomeFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_staff_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs     = requireContext().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        val name      = prefs.getString("user_name", "Staff") ?: "Staff"
        val firstName = name.substringBefore(" ").ifEmpty { "Staff" }
        view.findViewById<TextView>(R.id.tv_staff_greeting)?.text = "Hello, $firstName!"

        // Load summary stats
        db.collection("orders").get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val all       = snapshot.documents
            val pending   = all.count { it.getString("approvalStatus").equals("pending", true) }
            val active    = all.count {
                val s = it.getString("status") ?: ""
                s.equals("confirmed", true) || s.equals("assigned", true) || s.equals("in_transit", true)
            }
            val delivered = all.count { it.getString("status").equals("delivered", true) }
            view.findViewById<TextView>(R.id.tv_pending_approvals)?.text = pending.toString()
            view.findViewById<TextView>(R.id.tv_active_orders)?.text     = active.toString()
            view.findViewById<TextView>(R.id.tv_delivered_today)?.text   = delivered.toString()
        }

        db.collection("users")
            .whereEqualTo("role", "rider")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                view.findViewById<TextView>(R.id.tv_available_riders)?.text = snapshot.size().toString()
            }

        // Quick action buttons
        view.findViewById<View>(R.id.btn_go_approval)?.setOnClickListener {
            (activity as? StaffMainActivity)?.navigateTo(StaffApprovalFragment())
        }
        view.findViewById<View>(R.id.btn_go_orders)?.setOnClickListener {
            (activity as? StaffMainActivity)?.navigateTo(StaffOrderFragment())
        }
    }
}