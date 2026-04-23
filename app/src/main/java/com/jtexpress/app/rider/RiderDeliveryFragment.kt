package com.jtexpress.app.rider

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R

class RiderDeliveryFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_rider_delivery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDeliveries(view)

        view.findViewById<View>(R.id.tab_active)?.setOnClickListener { loadDeliveries(view, "active") }
        view.findViewById<View>(R.id.tab_completed)?.setOnClickListener { loadDeliveries(view, "completed") }
    }

    private fun loadDeliveries(view: View, filter: String = "active") {
        val uid = auth.currentUser?.uid ?: return
        val container = view.findViewById<LinearLayout>(R.id.delivery_list_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_no_deliveries)

        container?.removeAllViews()

        val query = db.collection("orders").whereEqualTo("riderId", uid)

        query.get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val filtered = snapshot.documents.filter { doc ->
                val s = doc.getString("status") ?: ""
                if (filter == "active") s.equals("assigned", true) || s.equals("in_transit", true)
                else s.equals("delivered", true)
            }

            if (filtered.isEmpty()) {
                tvEmpty?.visibility = View.VISIBLE
                tvEmpty?.text = if (filter == "active") "No active deliveries." else "No completed deliveries."
                return@addOnSuccessListener
            }
            tvEmpty?.visibility = View.GONE

            filtered.forEach { doc ->
                val card = buildDeliveryCard(
                    tracking   = doc.getString("trackingNumber") ?: doc.id,
                    recipient  = "${doc.getString("recipientFirstName")} ${doc.getString("recipientLastName")}",
                    address    = "${doc.getString("recipientStreet")}, ${doc.getString("recipientCity")}",
                    status     = doc.getString("status") ?: "assigned",
                    orderId    = doc.id
                )
                container?.addView(card)
            }
        }
    }

    private fun buildDeliveryCard(
        tracking: String, recipient: String, address: String,
        status: String, orderId: String
    ): View {
        val ctx = requireContext()
        val dm  = resources.displayMetrics
        val dp  = dm.density

        val card = CardView(ctx).apply {
            radius        = 12f * dp
            cardElevation = 3f * dp
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.jt_white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (10 * dp).toInt()) }
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * dp).toInt(); setPadding(p, p, p, p)
        }

        fun tv(text: String, bold: Boolean = false, color: Int = R.color.jt_black, size: Float = 13f) =
            TextView(ctx).apply {
                this.text = text; textSize = size
                setTextColor(ContextCompat.getColor(ctx, color))
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (4 * dp).toInt() }
            }

        inner.addView(tv(tracking, bold = true, size = 15f))
        inner.addView(tv(recipient))
        inner.addView(tv(address, color = R.color.jt_gray))
        inner.addView(tv(status.uppercase(), bold = true, color = if (status == "delivered") android.R.color.holo_green_dark else R.color.jt_red, size = 11f))

        // Mark as Picked Up / Delivered buttons
        if (status.equals("assigned", true)) {
            val btnPickup = androidx.appcompat.widget.AppCompatButton(ctx).apply {
                text = "Mark as Picked Up"
                textSize = 13f
                setTextColor(android.graphics.Color.WHITE)
                isAllCaps = false
                setBackgroundResource(R.drawable.bg_btn_red)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (40 * dp).toInt()
                ).apply { topMargin = (8 * dp).toInt() }
                setOnClickListener { updateOrderStatus(orderId, "in_transit") }
            }
            inner.addView(btnPickup)
        }
        if (status.equals("in_transit", true)) {
            val btnDeliver = androidx.appcompat.widget.AppCompatButton(ctx).apply {
                text = "Mark as Delivered"
                textSize = 13f
                setTextColor(android.graphics.Color.WHITE)
                isAllCaps = false
                setBackgroundResource(R.drawable.bg_btn_red)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (40 * dp).toInt()
                ).apply { topMargin = (8 * dp).toInt() }
                setOnClickListener { updateOrderStatus(orderId, "delivered") }
            }
            inner.addView(btnDeliver)
        }

        card.addView(inner)
        return card
    }

    private fun updateOrderStatus(orderId: String, newStatus: String) {
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        if (newStatus == "delivered") {
            updates["deliveredAt"] = com.google.firebase.Timestamp.now()
        }
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
            .update(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    android.widget.Toast.makeText(requireContext(), "Status updated!", android.widget.Toast.LENGTH_SHORT).show()
                    loadDeliveries(requireView())
                }
            }
    }
}