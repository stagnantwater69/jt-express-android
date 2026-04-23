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
class RiderEarningsFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_rider_earnings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = auth.currentUser?.uid ?: return

        db.collection("orders")
            .whereEqualTo("riderId", uid)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                var totalEarnings = 0.0
                snapshot.documents.forEach { doc ->
                    // Rider earns 15% of shipping fee
                    val fee = doc.getDouble("shippingFee") ?: 0.0
                    totalEarnings += fee * 0.15
                }
                view.findViewById<TextView>(R.id.tv_total_earnings)?.text =
                    "₱ ${"%.2f".format(totalEarnings)}"
                view.findViewById<TextView>(R.id.tv_deliveries_count)?.text =
                    snapshot.size().toString()
            }
    }
}