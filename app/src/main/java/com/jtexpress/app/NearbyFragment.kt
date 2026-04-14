package com.jtexpress.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class NearbyFragment : Fragment() {

    private var activeChip = "all"
    private var db: FirebaseFirestore? = null
    private var allBranches = listOf<Branch>()
    private var selectedMapBranch: Branch? = null

    data class Branch(
        val id: String,
        val name: String,
        val code: String,
        val type: String,
        val city: String,
        val address: String,
        val hours: String,
        val phone: String,
        val isOpen: Boolean,
        val note: String,
        val location: GeoPoint,
        val order: Long
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_nearby, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        setupChips(view)
        setupSearch(view)
        setupMapToggle(view)
        loadBranches(view)
    }

    // ── Load branches from Firestore ──────────────────────────────
    private fun loadBranches(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.branches_container)
        val tvCount   = view.findViewById<TextView>(R.id.tv_result_count)

        container.removeAllViews()
        tvCount.text = "Loading branches..."

        db!!.collection("branches")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                allBranches = snapshot.documents.mapNotNull { doc ->
                    try {
                        Branch(
                            id       = doc.id,
                            name     = doc.getString("name").orEmpty(),
                            code     = doc.getString("code").orEmpty(),
                            type     = doc.getString("type").orEmpty(),
                            city     = doc.getString("city").orEmpty(),
                            address  = doc.getString("address").orEmpty(),
                            hours    = doc.getString("hours").orEmpty(),
                            phone    = doc.getString("phone").orEmpty(),
                            isOpen   = doc.getBoolean("isOpen") ?: false,
                            note     = doc.getString("note").orEmpty(),
                            location = doc.getGeoPoint("location") ?: GeoPoint(0.0, 0.0),
                            order    = doc.getLong("order") ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.e("NearbyFragment", "Branch parse error: ${e.message}")
                        null
                    }
                }.sortedBy { it.order }

                renderBranches(view, allBranches)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Log.e("NearbyFragment", "Failed to load branches: ${e.message}")
                tvCount.text = "Could not load branches. Check your connection."
            }
    }

    // ── Render branch cards dynamically ───────────────────────────
    private fun renderBranches(view: View, branches: List<Branch>) {
        val container = view.findViewById<LinearLayout>(R.id.branches_container)
        val tvCount   = view.findViewById<TextView>(R.id.tv_result_count)

        container.removeAllViews()
        tvCount.text = "${branches.size} drop point${if (branches.size != 1) "s" else ""} found near you"

        if (branches.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text     = "No branches found."
                textSize = 14f
                gravity  = Gravity.CENTER
                setPadding(0, 48, 0, 0)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_gray))
            }
            container.addView(tv)
            return
        }

        branches.forEach { branch ->
            container.addView(buildBranchCard(branch))
        }
    }

    // ── Build a single branch card programmatically ───────────────
    private fun buildBranchCard(branch: Branch): CardView {
        val ctx = requireContext()
        val dp  = resources.displayMetrics.density

        val (badgeLabel, badgeBg, badgeColor) = when (branch.type) {
            "home"  -> Triple("J&T Home",   R.drawable.badge_status_transit,   "#B71C1C")
            "store" -> Triple("J&T Store",  R.drawable.badge_status_delivered, "#1B5E20")
            else    -> Triple("Drop Point", R.drawable.badge_status_pending,   "#BF360C")
        }

        // Root card
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

        // ── Row 1: Badge + Open/Closed ────────────────────────────
        val row1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
        }

        val tvBadge = TextView(ctx).apply {
            text      = badgeLabel
            textSize  = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(badgeColor))
            setBackgroundResource(badgeBg)
            val hp = (10 * dp).toInt(); val vp = (4 * dp).toInt()
            setPadding(hp, vp, hp, vp)
        }

        val tvOpenStatus = TextView(ctx).apply {
            text      = if (branch.isOpen) "  •  Open Now" else "  •  Closed"
            textSize  = 11f
            setTextColor(
                if (branch.isOpen) android.graphics.Color.parseColor("#2E7D32")
                else ContextCompat.getColor(ctx, R.color.jt_gray)
            )
        }

        val spacer = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        row1.addView(tvBadge)
        row1.addView(tvOpenStatus)
        row1.addView(spacer)

        // ── Branch name ───────────────────────────────────────────
        val tvName = TextView(ctx).apply {
            text      = branch.name
            textSize  = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
        }

        // ── Code + city row ───────────────────────────────────────
        val rowCode = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * dp).toInt() }
        }
        val tvCode = TextView(ctx).apply {
            text      = branch.code
            textSize  = 11f
            setTextColor(android.graphics.Color.parseColor(badgeColor))
            setBackgroundResource(badgeBg)
            val hp = (6 * dp).toInt(); val vp = (2 * dp).toInt()
            setPadding(hp, vp, hp, vp)
        }
        val tvCity = TextView(ctx).apply {
            text      = "  ${branch.city}"
            textSize  = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
        }
        rowCode.addView(tvCode)
        rowCode.addView(tvCity)

        // ── Divider ───────────────────────────────────────────────
        val divider = View(ctx).apply {
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.jt_divider))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply {
                topMargin    = (10 * dp).toInt()
                bottomMargin = (10 * dp).toInt()
            }
        }

        // ── Action buttons ────────────────────────────────────────
        val rowBtns = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * dp).toInt() }
        }

        val btnDir = AppCompatButton(ctx).apply {
            text      = "Directions"
            textSize  = 13f
            setTextColor(android.graphics.Color.WHITE)
            isAllCaps = false
            setBackgroundResource(R.drawable.bg_btn_red)
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f).apply {
                marginEnd = (8 * dp).toInt()
            }
            setOnClickListener { openDirections(branch) }
        }

        val btnCall = AppCompatButton(ctx).apply {
            text      = "Call"
            textSize  = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.jt_red))
            isAllCaps = false
            setBackgroundResource(R.drawable.bg_btn_outline_red)
            layoutParams = LinearLayout.LayoutParams(0, (40 * dp).toInt(), 1f)
            setOnClickListener { dialPhone(branch.phone) }
        }

        rowBtns.addView(btnDir)
        rowBtns.addView(btnCall)

        // ── Assemble ──────────────────────────────────────────────
        inner.addView(row1)
        inner.addView(tvName)
        inner.addView(rowCode)
        inner.addView(divider)
        inner.addView(makeIconTextRow(branch.address, android.R.drawable.ic_menu_myplaces))
        inner.addView(makeIconTextRow(branch.hours,   android.R.drawable.ic_menu_recent_history))
        inner.addView(makeIconTextRow(branch.phone,   android.R.drawable.ic_menu_call))

        if (branch.note.isNotEmpty()) {
            inner.addView(TextView(ctx).apply {
                text      = branch.note
                textSize  = 11f
                setTextColor(ContextCompat.getColor(ctx, R.color.jt_gray))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (6 * dp).toInt() }
                setLineSpacing(0f, 1.4f)  // ← fixed
            })
        }

        inner.addView(rowBtns)
        card.addView(inner)
        card.setOnClickListener { showOnMap(requireView(), branch) }

        return card
    }

    // ── Helper: icon + text row ───────────────────────────────────
    private fun makeIconTextRow(text: String, iconRes: Int): LinearLayout {
        val ctx = requireContext()
        val dp  = resources.displayMetrics.density

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * dp).toInt() }

            addView(ImageView(ctx).apply {
                setImageResource(iconRes)
                imageTintList = ContextCompat.getColorStateList(ctx, R.color.jt_gray)
                layoutParams  = LinearLayout.LayoutParams((14 * dp).toInt(), (14 * dp).toInt())
            })
            addView(TextView(ctx).apply {
                this.text = "  $text"
                textSize  = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.jt_black))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            })
        }
    }

    // ── Map toggle ────────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapToggle(view: View) {
        val listView = view.findViewById<View>(R.id.layout_list_view)
        val mapView  = view.findViewById<View>(R.id.layout_map_view)
        val webView  = view.findViewById<WebView>(R.id.map_webview)

        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled    = true
            loadWithOverviewMode = true
            useWideViewPort      = true
            setSupportZoom(true)
            builtInZoomControls  = true
            displayZoomControls  = false
            cacheMode            = WebSettings.LOAD_NO_CACHE
        }

        view.findViewById<ImageButton>(R.id.btn_use_location).setOnClickListener {
            val branch = allBranches.firstOrNull() ?: return@setOnClickListener
            showOnMap(view, branch)
        }

        view.findViewById<ImageButton>(R.id.btn_back_to_list).setOnClickListener {
            mapView.visibility  = View.GONE
            listView.visibility = View.VISIBLE
            webView.loadUrl("about:blank")
        }

        view.findViewById<ImageButton>(R.id.map_btn_navigate).setOnClickListener {
            selectedMapBranch?.let { openDirections(it) }
        }

        view.findViewById<ImageButton>(R.id.map_btn_call).setOnClickListener {
            selectedMapBranch?.let { dialPhone(it.phone) }
        }
    }

    private fun showOnMap(view: View, branch: Branch) {
        selectedMapBranch = branch
        val listView = view.findViewById<View>(R.id.layout_list_view)
        val mapView  = view.findViewById<View>(R.id.layout_map_view)
        val webView  = view.findViewById<WebView>(R.id.map_webview)

        val lat = branch.location.latitude
        val lng = branch.location.longitude
        val mapUrl = "https://www.openstreetmap.org/export/embed.html" +
                "?bbox=${lng - 0.01},${lat - 0.01},${lng + 0.01},${lat + 0.01}" +
                "&layer=mapnik&marker=$lat,$lng"

        webView.loadUrl(mapUrl)
        populateBottomSheet(view, branch)
        listView.visibility = View.GONE
        mapView.visibility  = View.VISIBLE
    }

    private fun populateBottomSheet(view: View, branch: Branch) {
        view.findViewById<TextView>(R.id.map_branch_name).text     = branch.name
        view.findViewById<TextView>(R.id.map_branch_code).text     = branch.code
        view.findViewById<TextView>(R.id.map_branch_distance).text = ""
        view.findViewById<TextView>(R.id.map_branch_hours).text    = "Operating Hours:  ${branch.hours}"
        view.findViewById<TextView>(R.id.map_branch_address).text  = "  ${branch.address}"
        view.findViewById<TextView>(R.id.map_branch_phone).text    = "  Phone: ${branch.phone}"
        view.findViewById<TextView>(R.id.map_branch_note).text     = branch.note
        view.findViewById<TextView>(R.id.map_branch_function).visibility =
            if (branch.type == "home") View.VISIBLE else View.GONE
    }

    // ── Filter chips ──────────────────────────────────────────────
    private fun setupChips(view: View) {
        val chipIds = mapOf(
            "all"   to R.id.chip_all,
            "drop"  to R.id.chip_drop,
            "home"  to R.id.chip_home,
            "store" to R.id.chip_store,
            "open"  to R.id.chip_open
        )
        chipIds.forEach { (key, id) ->
            view.findViewById<TextView>(id).setOnClickListener {
                activeChip = key
                updateChipStyles(view, chipIds, key)
                applyFilter(view)
            }
        }
    }

    private fun updateChipStyles(view: View, chipIds: Map<String, Int>, activeKey: String) {
        chipIds.forEach { (key, id) ->
            val chip = view.findViewById<TextView>(id)
            if (key == activeKey) {
                chip.setBackgroundResource(R.drawable.chip_active_bg)
                chip.setTextColor(resources.getColor(android.R.color.white, null))
            } else {
                chip.setBackgroundResource(R.drawable.chip_inactive_bg)
                chip.setTextColor(resources.getColor(R.color.jt_gray, null))
            }
        }
    }

    private fun applyFilter(view: View) {
        val query = view.findViewById<EditText>(R.id.et_location_search)
            .text.toString().lowercase().trim()

        val filtered = allBranches.filter { branch ->
            val matchesChip = when (activeChip) {
                "drop"  -> branch.type == "drop"
                "home"  -> branch.type == "home"
                "store" -> branch.type == "store"
                "open"  -> branch.isOpen
                else    -> true
            }
            val matchesSearch = query.isEmpty()
                    || branch.name.lowercase().contains(query)
                    || branch.address.lowercase().contains(query)
                    || branch.city.lowercase().contains(query)
            matchesChip && matchesSearch
        }
        renderBranches(view, filtered)
    }

    // ── Search ────────────────────────────────────────────────────
    private fun setupSearch(view: View) {
        view.findViewById<EditText>(R.id.et_location_search)
            .addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { applyFilter(view) }
                override fun afterTextChanged(s: Editable?) {}
            })
    }

    // ── Helpers ───────────────────────────────────────────────────
    private fun openDirections(branch: Branch) {
        val lat    = branch.location.latitude
        val lng    = branch.location.longitude
        val uri    = Uri.parse("geo:$lat,$lng?q=${Uri.encode(branch.address)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=${Uri.encode(branch.address)}")))
        }
    }

    private fun dialPhone(phone: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+63$phone")))
    }
}