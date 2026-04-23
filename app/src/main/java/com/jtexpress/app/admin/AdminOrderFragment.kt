package com.jtexpress.app.admin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jtexpress.app.R

class AdminOrderFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.admin_orders_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_admin_no_orders)

        db.collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (snapshot.isEmpty) {
                    tvEmpty?.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                tvEmpty?.visibility = View.GONE
                container?.removeAllViews()
                val dp = resources.displayMetrics.density

                snapshot.documents.forEach { doc ->
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        val p = (14 * dp).toInt()
                        setPadding(p, p, p, p)
                        setBackgroundColor(Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = (1 * dp).toInt() }
                    }

                    val col = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    col.addView(TextView(requireContext()).apply {
                        text = doc.getString("trackingNumber") ?: doc.id
                        textSize = 13f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_black))
                    })
                    col.addView(TextView(requireContext()).apply {
                        text = "${doc.getString("senderCity")} → ${doc.getString("recipientCity")}"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_gray))
                    })

                    row.addView(col)
                    row.addView(TextView(requireContext()).apply {
                        text = (doc.getString("status") ?: "pending").uppercase()
                        textSize = 10f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_red))
                    })

                    container?.addView(row)
                }
            }
    }
}