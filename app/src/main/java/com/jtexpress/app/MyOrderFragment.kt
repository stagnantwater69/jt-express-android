package com.jtexpress.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import com.google.firebase.firestore.Query
import java.util.Locale

class MyOrderFragment : Fragment() {

    private var selectedTab = "all"
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    // Store all fetched orders so we can filter without re-fetching
    private val allOrders = mutableListOf<OrderItem>()

    data class OrderItem(
        val id: String,
        val trackingNumber: String,
        val status: String,
        val type: String,       // "send" or "receive"
        val origin: String,
        val destination: String,
        val estimatedDelivery: String,
        val createdAt: String
    )

    private var orderContainer: LinearLayout? = null
    private var tvEmpty: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_order, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        orderContainer = view.findViewById(R.id.order_container)
        tvEmpty        = view.findViewById(R.id.tv_no_orders)

        setupTabs(view)
        setupSearch(view)
        loadOrdersFromFirestore(view)
    }

    // ── Load orders from Firestore ─────────────────────────────
    private fun loadOrdersFromFirestore(view: View) {
        val uid = auth?.currentUser?.uid ?: return

        db!!.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                allOrders.clear()

                snapshot.documents
                    .sortedByDescending { doc ->
                        try { doc.getTimestamp("createdAt")?.seconds ?: 0L }
                        catch (e: Exception) { 0L }
                    }
                    .forEach { doc ->
                        allOrders.add(
                            OrderItem(
                                id                = doc.id,
                                trackingNumber    = doc.getString("trackingNumber") ?: doc.id,
                                status            = doc.getString("status").orEmpty().ifEmpty { "Pending" },
                                type              = doc.getString("type").orEmpty().ifEmpty { "send" },
                                origin            = doc.getString("origin").orEmpty().ifEmpty { "—" },
                                destination       = doc.getString("destination").orEmpty().ifEmpty { "—" },
                                estimatedDelivery = doc.getString("estimatedDelivery").orEmpty(),
                                createdAt         = getCreatedAt(doc)
                            )
                        )
                    }

                updateTabCounts(view)
                renderOrders(allOrders)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Log.e("MyOrderFragment", "Failed to load orders: ${e.message}")
                tvEmpty?.visibility = View.VISIBLE
                tvEmpty?.text = "Could not load orders. Check your connection."
            }
    }

    private fun getCreatedAt(doc: com.google.firebase.firestore.DocumentSnapshot): String {
        return try {
            val timestamp = doc.getTimestamp("createdAt")
            if (timestamp != null) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(timestamp.toDate())
            } else {
                doc.getString("createdAt").orEmpty()
            }
        } catch (e: Exception) {
            ""
        }
    }

    // ── Update tab count badges ────────────────────────────────
    private fun updateTabCounts(view: View) {
        view.findViewById<TextView>(R.id.count_all).text     = allOrders.size.toString()
        view.findViewById<TextView>(R.id.count_send).text    = allOrders.count { it.type == "send" }.toString()
        view.findViewById<TextView>(R.id.count_receive).text = allOrders.count { it.type == "receive" }.toString()
        view.findViewById<TextView>(R.id.count_pending).text = allOrders.count {
            it.status.equals("Pending", ignoreCase = true) ||
                    it.status.equals("Pending Pickup", ignoreCase = true)
        }.toString()
    }

    // ── Render order cards dynamically ─────────────────────────
    private fun renderOrders(orders: List<OrderItem>) {
        orderContainer?.removeAllViews()

        if (orders.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            return
        }

        tvEmpty?.visibility = View.GONE
        orders.forEach { order ->
            orderContainer?.addView(buildOrderCard(order))
        }
    }

    // ── Build a single order card ──────────────────────────────
    private fun buildOrderCard(order: OrderItem): View {
        val ctx = requireContext()
        val dm  = resources.displayMetrics
        val dp  = dm.density

        // Card
        val card = CardView(ctx).apply {
            radius        = 14f * dp
            cardElevation = 3f * dp
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.jt_white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (10 * dp).toInt()) }
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * dp).toInt()
            setPadding(p, p, p, p)
        }

        // ── Row 1: type badge + date ───────────────────────────
        val row1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (8 * dp).toInt()) }
        }

        val (badgeBg, badgeColor) = when (order.status.lowercase()) {
            "delivered"                      -> Pair(R.drawable.badge_status_delivered, android.R.color.holo_green_dark)
            "in transit", "out for delivery" -> Pair(R.drawable.badge_status_transit, R.color.jt_red)
            else                             -> Pair(R.drawable.badge_status_pending, R.color.jt_black)
        }

        val tvBadge = TextView(ctx).apply {
            text = order.type.uppercase()
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, badgeColor))
            background = ContextCompat.getDrawable(ctx, badgeBg)
            val hp = (10 * dp).toInt(); val vp = (4 * dp).toInt()
            setPadding(hp, vp, hp, vp)
        }

        val spacer1 = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        val tvDate = TextView(ctx).apply {
            text = order.createdAt
            textSize = 11f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
        }

        row1.addView(tvBadge)
        row1.addView(spacer1)
        row1.addView(tvDate)

        // ── Row 2: tracking number + status ───────────────────
        val row2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (8 * dp).toInt()) }
        }

        val tvTracking = TextView(ctx).apply {
            text = order.trackingNumber
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val statusColor = when (order.status.lowercase()) {
            "delivered"                      -> android.graphics.Color.parseColor("#2E7D32")
            "in transit", "out for delivery" -> ContextCompat.getColor(ctx, R.color.jt_red)
            else                             -> android.graphics.Color.parseColor("#E65100")
        }

        val statusText = when (order.status.lowercase()) {
            "delivered" -> "Delivered ✓"
            else        -> order.status
        }

        val tvStatus = TextView(ctx).apply {
            text = statusText
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(statusColor)
        }

        row2.addView(tvTracking)
        row2.addView(tvStatus)

        // ── Row 3: route + eta ─────────────────────────────────
        val row3 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (10 * dp).toInt()) }
        }

        val tvOrigin = TextView(ctx).apply {
            text = order.origin
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
        }

        val tvArrow = TextView(ctx).apply {
            text = "  →  "
            textSize = 13f
            setTextColor(statusColor)
        }

        val tvDest = TextView(ctx).apply {
            text = order.destination
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val etaText = order.estimatedDelivery.ifEmpty {
            when (order.status.lowercase()) {
                "delivered" -> "Delivered"
                "pending", "pending pickup" -> "Awaiting"
                else -> "—"
            }
        }

        val tvEta = TextView(ctx).apply {
            text = etaText
            textSize = 11f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
        }

        row3.addView(tvOrigin)
        row3.addView(tvArrow)
        row3.addView(tvDest)
        row3.addView(tvEta)

        // ── Progress bar ───────────────────────────────────────
        val progress = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            this.progress = getProgressForStatus(order.status)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (7 * dp).toInt()
            ).apply { setMargins(0, 0, 0, (12 * dp).toInt()) }
            progressTintList = android.content.res.ColorStateList.valueOf(statusColor)
        }

        // ── Buttons ────────────────────────────────────────────
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnTrack = AppCompatButton(ctx).apply {
            text = "Track"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            isAllCaps = false
            background = ContextCompat.getDrawable(ctx, R.drawable.bg_btn_red)
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f).apply {
                setMargins(0, 0, (8 * dp).toInt(), 0)
            }
            setOnClickListener {
                Toast.makeText(ctx, "Tracking ${order.trackingNumber}…", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCopy = AppCompatButton(ctx).apply {
            text = "Copy No."
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_red))
            isAllCaps = false
            background = ContextCompat.getDrawable(ctx, R.drawable.bg_btn_outline_red)
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f)
            setOnClickListener { copyToClipboard(order.trackingNumber) }
        }

        btnRow.addView(btnTrack)
        btnRow.addView(btnCopy)

        // Assemble
        inner.addView(row1)
        inner.addView(row2)
        inner.addView(row3)
        inner.addView(progress)
        inner.addView(btnRow)
        card.addView(inner)

        return card
    }

    private fun getProgressForStatus(status: String): Int = when (status.lowercase()) {
        "pending", "pending pickup" -> 10
        "picked up"                 -> 30
        "in transit"                -> 60
        "out for delivery"          -> 85
        "delivered"                 -> 100
        else                        -> 0
    }

    // ── Tabs ───────────────────────────────────────────────────
    private fun setupTabs(view: View) {
        val tabs = listOf(
            view.findViewById<LinearLayout>(R.id.tab_all)     to "all",
            view.findViewById<LinearLayout>(R.id.tab_send)    to "send",
            view.findViewById<LinearLayout>(R.id.tab_receive) to "receive",
            view.findViewById<LinearLayout>(R.id.tab_pending) to "pending"
        )

        val indicators = mapOf(
            "all"     to view.findViewById<View>(R.id.indicator_all),
            "send"    to view.findViewById<View>(R.id.indicator_send),
            "receive" to view.findViewById<View>(R.id.indicator_receive),
            "pending" to view.findViewById<View>(R.id.indicator_pending)
        )

        tabs.forEach { (tabLayout, key) ->
            tabLayout.setOnClickListener {
                selectedTab = key

                indicators.forEach { (k, indicator) ->
                    indicator.setBackgroundColor(
                        if (k == key) resources.getColor(R.color.jt_red, null)
                        else android.graphics.Color.TRANSPARENT
                    )
                }

                tabs.forEach { (layout, tabKey) ->
                    val color = if (tabKey == key)
                        resources.getColor(R.color.jt_red, null)
                    else
                        resources.getColor(R.color.jt_gray, null)
                    for (i in 0 until layout.childCount) {
                        val child = layout.getChildAt(i)
                        if (child is TextView) child.setTextColor(color)
                    }
                }

                // Filter from cached list — no re-fetch needed
                val filtered = when (key) {
                    "send"    -> allOrders.filter { it.type == "send" }
                    "receive" -> allOrders.filter { it.type == "receive" }
                    "pending" -> allOrders.filter {
                        it.status.equals("Pending", ignoreCase = true) ||
                                it.status.equals("Pending Pickup", ignoreCase = true)
                    }
                    else -> allOrders
                }
                renderOrders(filtered)
            }
        }
    }

    // ── Search ─────────────────────────────────────────────────
    private fun setupSearch(view: View) {
        view.findViewById<AppCompatButton>(R.id.btn_search).setOnClickListener {
            val query = view.findViewById<TextInputEditText>(R.id.et_search)
                .text.toString().trim().uppercase()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a tracking number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val results = allOrders.filter { it.trackingNumber.contains(query) }
            if (results.isEmpty()) {
                Toast.makeText(requireContext(), "No orders found for: $query", Toast.LENGTH_SHORT).show()
            } else {
                renderOrders(results)
            }
        }
    }

    private fun copyToClipboard(trackingNo: String) {
        val clipboard = requireContext()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Tracking Number", trackingNo))
        Toast.makeText(requireContext(), "Copied: $trackingNo", Toast.LENGTH_SHORT).show()
    }
}