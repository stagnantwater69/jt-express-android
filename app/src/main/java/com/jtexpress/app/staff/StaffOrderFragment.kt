package com.jtexpress.app.staff

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

class StaffOrderFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_staff_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOrders(view)
    }

    private fun loadOrders(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.staff_orders_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_staff_no_orders)

        db.collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (snapshot.isEmpty) {
                    tvEmpty?.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                tvEmpty?.visibility = View.GONE
                container?.removeAllViews()

                snapshot.documents.forEach { doc ->
                    val row = buildOrderRow(
                        tracking = doc.getString("trackingNumber") ?: doc.id,
                        approval = doc.getString("approvalStatus") ?: "pending",
                        from     = doc.getString("senderCity") ?: "—",
                        to       = doc.getString("recipientCity") ?: "—"
                    )
                    container?.addView(row)
                }
            }
    }

    private fun buildOrderRow(
        tracking: String,
        approval: String,
        from: String,
        to: String
    ): LinearLayout {
        val ctx = requireContext()
        val dp  = resources.displayMetrics.density

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            val p = (12 * dp).toInt()
            setPadding(p, p, p, p)
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (1 * dp).toInt() }

            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            col.addView(TextView(ctx).apply {
                text = tracking
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
                setTypeface(null, Typeface.BOLD)
            })

            col.addView(TextView(ctx).apply {
                text = "$from → $to"
                textSize = 11f
                setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
            })

            addView(col)

            addView(TextView(ctx).apply {
                text = approval.uppercase()
                textSize = 10f
                setTextColor(
                    ContextCompat.getColor(ctx, when (approval.lowercase()) {
                        "approved" -> android.R.color.holo_green_dark
                        "rejected" -> R.color.jt_red
                        else       -> R.color.jt_black
                    })
                )
            })
        }
    }
}