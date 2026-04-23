package com.jtexpress.app.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.AdminAuditFragment
import com.jtexpress.app.AdminUserFragment
import com.jtexpress.app.R

class AdminHomeFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        val name  = prefs.getString("user_name", "Admin") ?: "Admin"
        view.findViewById<TextView>(R.id.tv_admin_greeting)?.text =
            "Admin Dashboard — Hello, ${name.substringBefore(" ")}!"

        // Total users
        db.collection("users").get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            view.findViewById<TextView>(R.id.tv_stat_total_users)?.text = snapshot.size().toString()
        }

        // Orders stats
        db.collection("orders").get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val docs      = snapshot.documents
            val total     = docs.size
            val pending   = docs.count { it.getString("approvalStatus").equals("pending", true) }
            val delivered = docs.count { it.getString("status").equals("delivered", true) }
            var revenue   = 0.0
            docs.filter { it.getString("status").equals("delivered", true) }
                .forEach { revenue += it.getDouble("totalAmount") ?: 0.0 }

            view.findViewById<TextView>(R.id.tv_stat_total_orders)?.text     = total.toString()
            view.findViewById<TextView>(R.id.tv_stat_pending_approval)?.text = pending.toString()
            view.findViewById<TextView>(R.id.tv_stat_total_delivered)?.text  = delivered.toString()
            view.findViewById<TextView>(R.id.tv_stat_revenue)?.text          = "₱ ${"%.2f".format(revenue)}"
        }

        // Active riders
        db.collection("users")
            .whereEqualTo("role", "rider")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                view.findViewById<TextView>(R.id.tv_stat_active_riders)?.text = snapshot.size().toString()
            }

        // Quick nav buttons
        view.findViewById<View>(R.id.btn_admin_users)?.setOnClickListener {
            (activity as? AdminMainActivity)?.navigateTo(AdminUserFragment())
        }
        view.findViewById<View>(R.id.btn_admin_reports)?.setOnClickListener {
            (activity as? AdminMainActivity)?.navigateTo(AdminReportsFragment())
        }
        view.findViewById<View>(R.id.btn_admin_audit)?.setOnClickListener {
            (activity as? AdminMainActivity)?.navigateTo(AdminAuditFragment())
        }
    }
}