package com.jtexpress.app.rider

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R

class RiderHomeFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_rider_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = auth.currentUser?.uid ?: return

        val prefs     = requireContext().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        val name      = prefs.getString("user_name", "Rider") ?: "Rider"
        val firstName = name.substringBefore(" ").ifEmpty { "Rider" }

        view.findViewById<TextView>(R.id.tv_rider_greeting)?.text = "Hello, $firstName!"

        // Load today's delivery stats
        db.collection("orders")
            .whereEqualTo("riderId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val all       = snapshot.documents
                val delivered = all.count { it.getString("status").equals("delivered", true) }
                val active    = all.count {
                    val s = it.getString("status") ?: ""
                    s.equals("assigned", true) || s.equals("in_transit", true)
                }
                view.findViewById<TextView>(R.id.tv_active_deliveries)?.text = active.toString()
                view.findViewById<TextView>(R.id.tv_completed_today)?.text   = delivered.toString()
                view.findViewById<TextView>(R.id.tv_total_deliveries)?.text  = all.size.toString()
            }
            .addOnFailureListener { e -> Log.e("RiderHome", "Stats load failed: ${e.message}") }

        // Latest active delivery
        db.collection("orders")
            .whereEqualTo("riderId", uid)
            .whereEqualTo("status", "assigned")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val doc         = snapshot.documents.firstOrNull()
                val cardCurrent = view.findViewById<CardView>(R.id.card_current_delivery)
                if (doc != null) {
                    cardCurrent?.visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tv_current_tracking)?.text =
                        doc.getString("trackingNumber") ?: doc.id
                    view.findViewById<TextView>(R.id.tv_current_recipient)?.text =
                        "${doc.getString("recipientFirstName")} ${doc.getString("recipientLastName")}"
                    view.findViewById<TextView>(R.id.tv_current_address)?.text =
                        "${doc.getString("recipientStreet")}, ${doc.getString("recipientCity")}"
                    cardCurrent?.setOnClickListener {
                        (activity as? RiderMainActivity)?.navigateTo(RiderDeliveryFragment())
                    }
                } else {
                    cardCurrent?.visibility = View.GONE
                }
            }

        // Navigate buttons
        view.findViewById<View>(R.id.btn_go_deliveries)?.setOnClickListener {
            (activity as? RiderMainActivity)?.navigateTo(RiderDeliveryFragment())
        }
        view.findViewById<View>(R.id.btn_go_earnings)?.setOnClickListener {
            (activity as? RiderMainActivity)?.navigateTo(RiderEarningsFragment())
        }
    }
}