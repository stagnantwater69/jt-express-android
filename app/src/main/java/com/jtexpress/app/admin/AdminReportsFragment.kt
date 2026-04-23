package com.jtexpress.app.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R

class AdminReportsFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_reports, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db.collection("orders").get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val docs      = snapshot.documents
            val total     = docs.size
            val delivered = docs.count { it.getString("status").equals("delivered", true) }
            val pending   = docs.count {
                it.getString("status").equals("pending_approval", true) ||
                        it.getString("approvalStatus").equals("pending", true)
            }
            val cancelled = docs.count { it.getString("status").equals("rejected", true) }
            var revenue   = 0.0
            docs.filter { it.getString("status").equals("delivered", true) }
                .forEach { revenue += it.getDouble("totalAmount") ?: 0.0 }

            val rate = if (total > 0) (delivered.toFloat() / total * 100).toInt() else 0

            view.findViewById<TextView>(R.id.tv_report_total)?.text     = total.toString()
            view.findViewById<TextView>(R.id.tv_report_delivered)?.text = delivered.toString()
            view.findViewById<TextView>(R.id.tv_report_pending)?.text   = pending.toString()
            view.findViewById<TextView>(R.id.tv_report_cancelled)?.text = cancelled.toString()
            view.findViewById<TextView>(R.id.tv_report_revenue)?.text   = "₱ ${"%.2f".format(revenue)}"
            view.findViewById<TextView>(R.id.tv_report_rate)?.text      = "$rate%"
        }
    }
}