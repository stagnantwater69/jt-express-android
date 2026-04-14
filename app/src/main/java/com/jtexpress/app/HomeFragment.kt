package com.jtexpress.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    // ── UI References ─────────────────────────────────────────────
    private var tvGreeting: TextView? = null
    private var tvOrderCount: TextView? = null
    private var bannerViewPager: ViewPager2? = null
    private var bannerDots: LinearLayout? = null
    private var cardActiveShipment: CardView? = null
    private var tvNoActiveShipment: TextView? = null
    private var tvActiveTrackingNo: TextView? = null
    private var tvActiveStatus: TextView? = null
    private var tvActiveOrigin: TextView? = null
    private var tvActiveDestination: TextView? = null
    private var tvActiveEstimate: TextView? = null
    private var progressDelivery: ProgressBar? = null
    private var btnViewDetails: MaterialButton? = null
    private var llRecentOrders: LinearLayout? = null
    private var tvNoRecentOrders: TextView? = null
    private var tvSeeAll: TextView? = null
    private var tvHotlineNumber: TextView? = null
    private var cardHotline: CardView? = null

    private var activeOrderId: String? = null

    // Auto-scroll handler for banner
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private var bannerItemCount = 0

    // ── Lifecycle ─────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        initViews(view)
        loadGreeting()
        loadBanners()
        loadOrdersData()
        loadHotlineNumber()
        setupClickListeners(view)
    }

    override fun onResume() {
        super.onResume()
        loadGreeting()
        loadOrdersData()
        startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
    }

    // ── Init views ────────────────────────────────────────────────
    private fun initViews(view: View) {
        tvGreeting          = view.findViewById(R.id.tv_greeting)
        tvOrderCount        = view.findViewById(R.id.tv_order_count)
        bannerViewPager     = view.findViewById(R.id.banner_viewpager)
        bannerDots          = view.findViewById(R.id.banner_dots)
        cardActiveShipment  = view.findViewById(R.id.card_active_shipment)
        tvNoActiveShipment  = view.findViewById(R.id.tv_no_active_shipment)
        tvActiveTrackingNo  = view.findViewById(R.id.tv_active_tracking_no)
        tvActiveStatus      = view.findViewById(R.id.tv_active_status)
        tvActiveOrigin      = view.findViewById(R.id.tv_active_origin)
        tvActiveDestination = view.findViewById(R.id.tv_active_destination)
        tvActiveEstimate    = view.findViewById(R.id.tv_active_estimate)
        progressDelivery    = view.findViewById(R.id.progress_delivery)
        btnViewDetails      = view.findViewById(R.id.btn_view_details)
        llRecentOrders      = view.findViewById(R.id.ll_recent_orders)
        tvNoRecentOrders    = view.findViewById(R.id.tv_no_recent_orders)
        tvSeeAll            = view.findViewById(R.id.tv_see_all)
        tvHotlineNumber     = view.findViewById(R.id.tv_hotline_number)
        cardHotline         = view.findViewById(R.id.card_hotline)
    }

    // ── 1. Greeting ───────────────────────────────────────────────
    private fun loadGreeting() {
        val prefs    = requireContext().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        val fullName = prefs.getString("user_name", "") ?: ""
        val email    = prefs.getString("user_email", "") ?: ""

        val firstName = when {
            fullName.isNotEmpty() -> fullName.substringBefore(" ").trim()
            email.isNotEmpty()    -> email.substringBefore("@").trim()
            else                  -> "there"
        }

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else      -> "Good evening"
        }

        tvGreeting?.text = "$timeGreeting, $firstName! 👋"
    }

    // ── 2. Load banners from Firestore ────────────────────────────
    //    Collection: banners
    //    Fields: title, subtitle, body, tag, order (number), active (bool)
    private fun loadBanners() {
        db!!.collection("banners")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val banners = snapshot.documents
                    .filter { it.getBoolean("active") == true }
                    .sortedBy { it.getLong("order") ?: 0L }
                    .map { doc ->
                        BannerAdapter.BannerItem(
                            title    = doc.getString("title").orEmpty(),
                            subtitle = doc.getString("subtitle").orEmpty(),
                            body     = doc.getString("body").orEmpty(),
                            tag      = doc.getString("tag").orEmpty()
                        )
                    }

                if (banners.isNotEmpty()) setupBannerCarousel(banners)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Banners failed: ${e.message}")
            }
    }

    private fun setupBannerCarousel(banners: List<BannerAdapter.BannerItem>) {
        bannerItemCount = banners.size
        val adapter = BannerAdapter(banners)
        bannerViewPager?.adapter = adapter

        bannerViewPager?.isNestedScrollingEnabled = false

        // Build dot indicators
        bannerDots?.removeAllViews()
        banners.forEachIndexed { index, _ ->
            val dot = View(requireContext()).apply {
                val dm     = resources.displayMetrics
                val w      = if (index == 0) (20 * dm.density).toInt() else (6 * dm.density).toInt()
                val h      = (6 * dm.density).toInt()
                val margin = (4 * dm.density).toInt()
                layoutParams = LinearLayout.LayoutParams(w, h).apply {
                    setMargins(0, 0, margin, 0)
                }
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (index == 0) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
            bannerDots?.addView(dot)
        }

        // Update dots on page change
        bannerViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position, banners.size)
                resetAutoScroll()
            }
        })

        startAutoScroll()
    }

    private fun updateDots(selectedIndex: Int, count: Int) {
        bannerDots?.let { container ->
            for (i in 0 until container.childCount) {
                val dot        = container.getChildAt(i)
                val dm         = resources.displayMetrics
                val isSelected = i == selectedIndex
                val w          = if (isSelected) (20 * dm.density).toInt() else (6 * dm.density).toInt()
                val h          = (6 * dm.density).toInt()
                val margin     = (4 * dm.density).toInt()
                dot.layoutParams = LinearLayout.LayoutParams(w, h).apply {
                    setMargins(0, 0, margin, 0)
                }
                dot.background = ContextCompat.getDrawable(
                    requireContext(),
                    if (isSelected) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
        }
    }

    private fun startAutoScroll() {
        if (bannerItemCount <= 1) return
        autoScrollRunnable = Runnable {
            val current = bannerViewPager?.currentItem ?: 0
            val next    = (current + 1) % bannerItemCount
            bannerViewPager?.setCurrentItem(next, true)
            autoScrollHandler.postDelayed(autoScrollRunnable!!, 3500)
        }
        autoScrollHandler.postDelayed(autoScrollRunnable!!, 3500)
    }

    private fun stopAutoScroll() {
        autoScrollRunnable?.let { autoScrollHandler.removeCallbacks(it) }
    }

    private fun resetAutoScroll() {
        stopAutoScroll()
        startAutoScroll()
    }

    // ── 3. Load orders from Firestore ─────────────────────────────
    private fun loadOrdersData() {
        val uid = auth?.currentUser?.uid ?: return

        db!!.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val allOrders = snapshot.documents
                    .sortedByDescending { doc ->
                        try { doc.getTimestamp("createdAt")?.seconds ?: 0L }
                        catch (e: Exception) { 0L }
                    }

                // Order count badge
                val total = allOrders.size
                tvOrderCount?.visibility = if (total > 0) View.VISIBLE else View.GONE
                tvOrderCount?.text = "$total"

                // Active shipment (first non-delivered/cancelled)
                val activeOrder = allOrders.firstOrNull { doc ->
                    val s = doc.getString("status").orEmpty()
                    !s.equals("Delivered", ignoreCase = true) &&
                            !s.equals("Cancelled", ignoreCase = true)
                }

                if (activeOrder != null) {
                    activeOrderId = activeOrder.id
                    val tracking    = activeOrder.getString("trackingNumber") ?: activeOrder.id
                    val status      = activeOrder.getString("status").orEmpty().ifEmpty { "Pending" }
                    val origin      = activeOrder.getString("origin").orEmpty().ifEmpty { "—" }
                    val destination = activeOrder.getString("destination").orEmpty().ifEmpty { "—" }
                    val estimate    = activeOrder.getString("estimatedDelivery").orEmpty()

                    tvActiveTrackingNo?.text   = tracking
                    tvActiveStatus?.text       = status
                    tvActiveOrigin?.text       = origin
                    tvActiveDestination?.text  = destination
                    progressDelivery?.progress = getProgressForStatus(status)

                    if (estimate.isNotEmpty()) {
                        tvActiveEstimate?.visibility = View.VISIBLE
                        tvActiveEstimate?.text       = "Est. delivery: $estimate"
                    } else {
                        tvActiveEstimate?.visibility = View.GONE
                    }

                    cardActiveShipment?.visibility = View.VISIBLE
                    tvNoActiveShipment?.visibility  = View.GONE
                } else {
                    activeOrderId                  = null
                    cardActiveShipment?.visibility = View.GONE
                    tvNoActiveShipment?.visibility  = View.VISIBLE
                    tvNoActiveShipment?.text        = "No active shipments right now."
                }

                // Recent deliveries (last 3)
                val recent = allOrders.filter { doc ->
                    doc.getString("status").orEmpty().equals("Delivered", ignoreCase = true)
                }.take(3)

                llRecentOrders?.removeAllViews()
                if (recent.isNotEmpty()) {
                    tvNoRecentOrders?.visibility = View.GONE
                    recent.forEach { doc ->
                        val view = buildRecentOrderView(
                            tracking    = doc.getString("trackingNumber") ?: doc.id,
                            status      = doc.getString("status").orEmpty(),
                            date        = doc.getString("estimatedDelivery").orEmpty(),
                            origin      = doc.getString("origin").orEmpty(),
                            destination = doc.getString("destination").orEmpty()
                        )
                        llRecentOrders?.addView(view)
                    }
                } else {
                    tvNoRecentOrders?.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                cardActiveShipment?.visibility = View.GONE
                tvNoActiveShipment?.visibility  = View.VISIBLE
                tvNoActiveShipment?.text        = "Could not load data. Check your connection."
                Log.e("HomeFragment", "Orders failed: ${e.message}")
            }
    }

    // ── Build recent order row ────────────────────────────────────
    private fun buildRecentOrderView(
        tracking: String,
        status: String,
        date: String,
        origin: String,
        destination: String
    ): View {
        val ctx = requireContext()
        val dm  = resources.displayMetrics

        val card = CardView(ctx).apply {
            radius        = 12f * dm.density
            cardElevation = 2f * dm.density
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.jt_white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (8 * dm.density).toInt()) }
        }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = android.view.Gravity.CENTER_VERTICAL
            val p = (14 * dm.density).toInt()
            setPadding(p, p, p, p)
        }

        val textCol = LinearLayout(ctx).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvTracking = TextView(ctx).apply {
            text      = tracking
            textSize  = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val route    = if (origin.isNotEmpty() && destination.isNotEmpty()) "$origin → $destination" else ""
        val subtitle = listOf(route, date).filter { it.isNotEmpty() }.joinToString(" • ")
            .ifEmpty { status }

        val tvSub = TextView(ctx).apply {
            text      = subtitle
            textSize  = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
            setPadding(0, (2 * dm.density).toInt(), 0, 0)
        }

        val tvStatus = TextView(ctx).apply {
            text      = status
            textSize  = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val hp = (8 * dm.density).toInt()
            val vp = (3 * dm.density).toInt()
            setPadding(hp, vp, hp, vp)
            val (colorRes, bgRes) = when (status.lowercase()) {
                "delivered"        -> Pair(android.R.color.holo_green_dark, R.drawable.badge_status_delivered)
                "in transit",
                "out for delivery" -> Pair(R.color.jt_red, R.drawable.badge_status_transit)
                else               -> Pair(R.color.jt_black, R.drawable.badge_status_pending)
            }
            setTextColor(ContextCompat.getColor(ctx, colorRes))
            background = ContextCompat.getDrawable(ctx, bgRes)
        }

        textCol.addView(tvTracking)
        textCol.addView(tvSub)
        row.addView(textCol)
        row.addView(tvStatus)
        card.addView(row)
        return card
    }

    private fun getProgressForStatus(status: String): Int = when (status.lowercase()) {
        "pending"          -> 10
        "picked up"        -> 30
        "in transit"       -> 60
        "out for delivery" -> 85
        "delivered"        -> 100
        else               -> 0
    }

    // ── 4. Hotline from Firestore ─────────────────────────────────
    private fun loadHotlineNumber() {
        db!!.collection("app_config").document("contact")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val number = doc?.getString("hotline").orEmpty()
                if (number.isNotEmpty()) tvHotlineNumber?.text = number
            }
    }

    // ── Click listeners ───────────────────────────────────────────
    private fun setupClickListeners(view: View) {

        view.findViewById<View>(R.id.btn_scanner)?.setOnClickListener {
            Toast.makeText(requireContext(), "Scanner — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_notification)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
        }

        // ── Order card → opens CreateOrderFragment via MainActivity.openCreateOrder()
        view.findViewById<CardView>(R.id.card_order)?.setOnClickListener {
            (activity as? MainActivity)?.openCreateOrder()
        }

        view.findViewById<CardView>(R.id.card_tracking)?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }

        view.findViewById<LinearLayout>(R.id.service_international)?.setOnClickListener {
            Toast.makeText(requireContext(), "International Shipping — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.service_vouchers)?.setOnClickListener {
            Toast.makeText(requireContext(), "Vouchers — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.service_platform_orders)?.setOnClickListener {
            Toast.makeText(requireContext(), "Platform Orders — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.service_cod)?.setOnClickListener {
            Toast.makeText(requireContext(), "COD Waybill Summary — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.service_claim_reports)?.setOnClickListener {
            Toast.makeText(requireContext(), "Claim Reports — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.service_request_logs)?.setOnClickListener {
            Toast.makeText(requireContext(), "Request Logs — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        btnViewDetails?.setOnClickListener {
            if (activeOrderId != null) {
                Toast.makeText(requireContext(), "Opening shipment details...", Toast.LENGTH_SHORT).show()
                // TODO: navigate to TrackingDetailActivity with activeOrderId
            } else {
                Toast.makeText(requireContext(), "No active shipment to view.", Toast.LENGTH_SHORT).show()
            }
        }

        tvSeeAll?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }

        cardHotline?.setOnClickListener {
            val number = tvHotlineNumber?.text?.toString().orEmpty()
            if (number.isNotEmpty() && number != "Hotline") {
                startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$number") })
            } else {
                Toast.makeText(requireContext(), "Hotline number unavailable.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}