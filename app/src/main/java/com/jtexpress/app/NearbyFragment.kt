package com.jtexpress.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class NearbyFragment : Fragment() {

    private var activeChip = "all"

    data class Branch(
        val name: String,
        val code: String,
        val type: String,
        val city: String,
        val address: String,
        val hours: String,
        val phone: String,
        val distance: String,
        val isOpen: Boolean,
        val note: String,
        val lat: Double,
        val lng: Double,
        val cardId: Int,
        val directionsId: Int,
        val callId: Int
    )

    private val branches = listOf(
        Branch(
            "H-MANDAUE CITY-TABOK-01 J&T Home", "PH04000012", "home",
            "MANDAUE-CITY", "Hi-way Tabok, Mandaue City",
            "9:30–18:00  1,2,3,4,5,6", "9421872457", "1.28 KM", true,
            "J&T Home only accepts regular walk-in customers and does not accept packages from J&T VIPs and E-Commerce Platforms (e.g. Shopee, Lazada, TikTok, etc.)",
            10.3512, 123.9320,
            R.id.branch_1, R.id.btn_directions_1, R.id.btn_call_1
        ),
        Branch(
            "H-MANDAUE CITY-CENTRO-03", "PH04000033", "drop",
            "MANDAUE-CITY", "National Highway, Centro, Mandaue",
            "8:00–19:00  1,2,3,4,5,6,7", "9321456789", "2.15 KM", true, "",
            10.3480, 123.9350,
            R.id.branch_2, R.id.btn_directions_2, R.id.btn_call_2
        ),
        Branch(
            "J&T STORE - TIPOLO MANDAUE", "PH04000076", "store",
            "MANDAUE-CITY", "Tipolo, Mandaue City, Cebu",
            "9:00–17:00  1,2,3,4,5", "9187654321", "3.40 KM", false, "",
            10.3440, 123.9290,
            R.id.branch_3, R.id.btn_directions_3, R.id.btn_call_3
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_nearby, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChips(view)
        setupSearch(view)
        setupListButtons(view)
        setupMapToggle(view)
    }

    // ── Map toggle (WebView — no Maps SDK required) ──────────────
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapToggle(view: View) {
        val listView = view.findViewById<View>(R.id.layout_list_view)
        val mapView  = view.findViewById<View>(R.id.layout_map_view)
        val webView  = view.findViewById<WebView>(R.id.map_webview)

        // Configure WebView
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        // Location button → show map view
        view.findViewById<ImageButton>(R.id.btn_use_location).setOnClickListener {
            val nearest = branches.first()
            // Load OpenStreetMap embed (no API key needed)
            val mapUrl = "https://www.openstreetmap.org/export/embed.html" +
                    "?bbox=${nearest.lng - 0.01},${nearest.lat - 0.01}," +
                    "${nearest.lng + 0.01},${nearest.lat + 0.01}" +
                    "&layer=mapnik" +
                    "&marker=${nearest.lat},${nearest.lng}"
            webView.loadUrl(mapUrl)

            populateBottomSheet(view, nearest)
            listView.visibility = View.GONE
            mapView.visibility  = View.VISIBLE
        }

        // Back button → return to list
        view.findViewById<ImageButton>(R.id.btn_back_to_list).setOnClickListener {
            mapView.visibility  = View.GONE
            listView.visibility = View.VISIBLE
            webView.loadUrl("about:blank") // stop map loading in background
        }

        // Navigate button in bottom sheet
        view.findViewById<ImageButton>(R.id.map_btn_navigate).setOnClickListener {
            val branch = branches.first()
            val uri = Uri.parse("geo:${branch.lat},${branch.lng}?q=${Uri.encode(branch.address)}")
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

        // Call button in bottom sheet
        view.findViewById<ImageButton>(R.id.map_btn_call).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:+63${branches.first().phone}")))
        }
    }

    private fun populateBottomSheet(view: View, branch: Branch) {
        view.findViewById<TextView>(R.id.map_branch_name).text    = branch.name
        view.findViewById<TextView>(R.id.map_branch_code).text    = branch.code
        view.findViewById<TextView>(R.id.map_branch_distance).text = branch.distance
        view.findViewById<TextView>(R.id.map_branch_hours).text   = "Operating Hours:  ${branch.hours}"
        view.findViewById<TextView>(R.id.map_branch_address).text = "  ${branch.address}"
        view.findViewById<TextView>(R.id.map_branch_phone).text   = "  Phone/Landline Number: ${branch.phone}"
        view.findViewById<TextView>(R.id.map_branch_note).text    = branch.note
        val fnView = view.findViewById<TextView>(R.id.map_branch_function)
        fnView.visibility = if (branch.type == "home") View.VISIBLE else View.GONE
    }

    // ── Filter chips ─────────────────────────────────────────────
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
                filterBranches(view, key)
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

    private fun filterBranches(view: View, filter: String) {
        var count = 0
        branches.forEach { branch ->
            val card = view.findViewById<CardView>(branch.cardId)
            val show = when (filter) {
                "drop"  -> branch.type == "drop"
                "home"  -> branch.type == "home"
                "store" -> branch.type == "store"
                "open"  -> branch.isOpen
                else    -> true
            }
            card.visibility = if (show) { count++; View.VISIBLE } else View.GONE
        }
        view.findViewById<TextView>(R.id.tv_result_count).text =
            "$count drop point${if (count != 1) "s" else ""} found near you"
    }

    // ── Search ───────────────────────────────────────────────────
    private fun setupSearch(view: View) {
        view.findViewById<EditText>(R.id.et_location_search).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                    val query = s.toString().lowercase().trim()
                    var count = 0
                    branches.forEach { branch ->
                        val card = view.findViewById<CardView>(branch.cardId)
                        val matches = query.isEmpty()
                                || branch.name.lowercase().contains(query)
                                || branch.address.lowercase().contains(query)
                                || branch.city.lowercase().contains(query)
                        card.visibility = if (matches) { count++; View.VISIBLE } else View.GONE
                    }
                    view.findViewById<TextView>(R.id.tv_result_count).text =
                        "$count drop point${if (count != 1) "s" else ""} found near you"
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    // ── List card buttons ────────────────────────────────────────
    private fun setupListButtons(view: View) {
        branches.forEach { branch ->
            view.findViewById<Button>(branch.directionsId).setOnClickListener {
                val uri = Uri.parse("geo:${branch.lat},${branch.lng}?q=${Uri.encode(branch.address)}")
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
            view.findViewById<Button>(branch.callId).setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:+63${branch.phone}")))
            }
        }
    }
}