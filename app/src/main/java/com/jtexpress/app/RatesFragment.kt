package com.jtexpress.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.ceil
import kotlin.math.max

class RatesFragment : Fragment() {

    private var weight = 1
    private var selectedService = "super"
    private var selectedCategory = "parcel"
    private var isInternational = false

    // Base rates per KG (₱) per service type
    private val rates = mapOf(
        "super"   to 60.0,
        "express" to 75.0,
        "economy" to 45.0
    )
    // Est. delivery days per service
    private val delivery = mapOf(
        "super"   to "1-2 days",
        "express" to "2-3 days",
        "economy" to "4-5 days"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_rates, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs(view)
        setupServiceType(view)
        setupCategory(view)
        setupWeight(view)
        setupDimensions(view)
        setupCitySelectors(view)
        setupCheckButton(view)
    }

    // ── Domestic / International tabs ──────────────────────────
    private fun setupTabs(view: View) {
        val tabDomestic = view.findViewById<LinearLayout>(R.id.tab_domestic)
        val tabIntl     = view.findViewById<LinearLayout>(R.id.tab_international)
        val indDomestic = view.findViewById<View>(R.id.indicator_domestic)
        val indIntl     = view.findViewById<View>(R.id.indicator_international)
        val tvDomestic  = tabDomestic.getChildAt(0) as TextView
        val tvIntl      = tabIntl.getChildAt(0) as TextView

        tabDomestic.setOnClickListener {
            isInternational = false
            indDomestic.setBackgroundColor(resources.getColor(R.color.jt_red, null))
            indIntl.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            tvDomestic.setTextColor(resources.getColor(R.color.jt_red, null))
            tvDomestic.typeface = android.graphics.Typeface.DEFAULT_BOLD
            tvIntl.setTextColor(resources.getColor(R.color.jt_gray, null))
            tvIntl.typeface = android.graphics.Typeface.DEFAULT
        }

        tabIntl.setOnClickListener {
            isInternational = true
            indIntl.setBackgroundColor(resources.getColor(R.color.jt_red, null))
            indDomestic.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            tvIntl.setTextColor(resources.getColor(R.color.jt_red, null))
            tvIntl.typeface = android.graphics.Typeface.DEFAULT_BOLD
            tvDomestic.setTextColor(resources.getColor(R.color.jt_gray, null))
            tvDomestic.typeface = android.graphics.Typeface.DEFAULT
            Toast.makeText(requireContext(), "International rates shown", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Service type selector ───────────────────────────────────
    private fun setupServiceType(view: View) {
        val services = mapOf(
            "super"   to view.findViewById<LinearLayout>(R.id.service_super),
            "express" to view.findViewById<LinearLayout>(R.id.service_express),
            "economy" to view.findViewById<LinearLayout>(R.id.service_economy)
        )
        val labels = mapOf(
            "super"   to view.findViewById<TextView>(R.id.tv_service_super),
            "express" to view.findViewById<TextView>(R.id.tv_service_express),
            "economy" to view.findViewById<TextView>(R.id.tv_service_economy)
        )

        services.forEach { (key, layout) ->
            layout.setOnClickListener {
                selectedService = key
                services.forEach { (k, v) ->
                    v.setBackgroundResource(
                        if (k == key) R.drawable.service_selected_bg
                        else R.drawable.service_unselected_bg
                    )
                }
                labels.forEach { (k, tv) ->
                    tv.setTextColor(
                        if (k == key) resources.getColor(R.color.jt_red, null)
                        else resources.getColor(R.color.jt_gray, null)
                    )
                    tv.typeface = if (k == key) android.graphics.Typeface.DEFAULT_BOLD
                    else android.graphics.Typeface.DEFAULT
                }
            }
        }
    }

    // ── Category selector ───────────────────────────────────────
    private fun setupCategory(view: View) {
        val cats = mapOf(
            "parcel"   to view.findViewById<LinearLayout>(R.id.cat_parcel),
            "document" to view.findViewById<LinearLayout>(R.id.cat_document),
            "fragile"  to view.findViewById<LinearLayout>(R.id.cat_fragile)
        )
        val labels = mapOf(
            "parcel"   to view.findViewById<TextView>(R.id.tv_cat_parcel),
            "document" to view.findViewById<TextView>(R.id.tv_cat_document),
            "fragile"  to view.findViewById<TextView>(R.id.tv_cat_fragile)
        )

        cats.forEach { (key, layout) ->
            layout.setOnClickListener {
                selectedCategory = key
                cats.forEach { (k, v) ->
                    v.setBackgroundResource(
                        if (k == key) R.drawable.service_selected_bg
                        else R.drawable.service_unselected_bg
                    )
                }
                labels.forEach { (k, tv) ->
                    tv.setTextColor(
                        if (k == key) resources.getColor(R.color.jt_red, null)
                        else resources.getColor(R.color.jt_gray, null)
                    )
                    tv.typeface = if (k == key) android.graphics.Typeface.DEFAULT_BOLD
                    else android.graphics.Typeface.DEFAULT
                }
            }
        }
    }

    // ── Weight +/- stepper ──────────────────────────────────────
    private fun setupWeight(view: View) {
        val tvWeight = view.findViewById<TextView>(R.id.tv_weight)

        // FIX: Changed from MaterialButton to AppCompatButton to match XML declaration
        view.findViewById<AppCompatButton>(R.id.btn_weight_minus).setOnClickListener {
            if (weight > 1) {
                weight--
                tvWeight.text = weight.toString()
            }
        }

        // FIX: Changed from MaterialButton to AppCompatButton to match XML declaration
        view.findViewById<AppCompatButton>(R.id.btn_weight_plus).setOnClickListener {
            if (weight < 100) {
                weight++
                tvWeight.text = weight.toString()
            }
        }
    }

    // ── Dimension fields → live dimensional weight ──────────────
    private fun setupDimensions(view: View) {
        val tvDimWeight = view.findViewById<TextView>(R.id.tv_dim_weight)
        val etL = view.findViewById<TextInputEditText>(R.id.et_length)
        val etW = view.findViewById<TextInputEditText>(R.id.et_width)
        val etH = view.findViewById<TextInputEditText>(R.id.et_height)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val l = etL.text.toString().toDoubleOrNull() ?: 0.0
                val w = etW.text.toString().toDoubleOrNull() ?: 0.0
                val h = etH.text.toString().toDoubleOrNull() ?: 0.0
                val dimWeight = ceil((l * w * h) / 5000.0)
                tvDimWeight.text = "${dimWeight.toInt()} KG"
            }
        }
        etL.addTextChangedListener(watcher)
        etW.addTextChangedListener(watcher)
        etH.addTextChangedListener(watcher)
    }

    // ── City selectors ──────────────────────────────────────────
    private fun setupCitySelectors(view: View) {
        val cities = listOf(
            "Manila", "Cebu", "Davao", "Quezon City",
            "Makati", "Pasig", "Taguig", "Mandaue", "Zamboanga"
        )

        view.findViewById<LinearLayout>(R.id.row_from).setOnClickListener {
            showCityPicker("FROM", cities) { city ->
                view.findViewById<TextView>(R.id.tv_from_city).apply {
                    text = city
                    setTextColor(resources.getColor(R.color.jt_black, null))
                }
            }
        }

        view.findViewById<LinearLayout>(R.id.row_to).setOnClickListener {
            showCityPicker("TO", cities) { city ->
                view.findViewById<TextView>(R.id.tv_to_city).apply {
                    text = city
                    setTextColor(resources.getColor(R.color.jt_black, null))
                }
            }
        }
    }

    private fun showCityPicker(label: String, cities: List<String>, onSelect: (String) -> Unit) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select $label City")
            .setItems(cities.toTypedArray()) { _, which ->
                onSelect(cities[which])
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    // ── Check Rate button ───────────────────────────────────────
    private fun setupCheckButton(view: View) {
        // FIX: Changed from MaterialButton to AppCompatButton to match XML declaration
        view.findViewById<AppCompatButton>(R.id.btn_check_rate).setOnClickListener {
            val fromCity = view.findViewById<TextView>(R.id.tv_from_city).text.toString()
            val toCity   = view.findViewById<TextView>(R.id.tv_to_city).text.toString()

            if (fromCity == "Select origin city" || toCity == "Select destination city") {
                Toast.makeText(requireContext(), "Please select origin and destination cities", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calculate dimensional weight
            val etL = view.findViewById<TextInputEditText>(R.id.et_length)
            val etW = view.findViewById<TextInputEditText>(R.id.et_width)
            val etH = view.findViewById<TextInputEditText>(R.id.et_height)
            val l = etL.text.toString().toDoubleOrNull() ?: 0.0
            val w = etW.text.toString().toDoubleOrNull() ?: 0.0
            val h = etH.text.toString().toDoubleOrNull() ?: 0.0
            val dimWeight = if (l > 0 && w > 0 && h > 0) ceil((l * w * h) / 5000.0).toInt() else 0

            // Chargeable = max of actual weight vs dimensional weight
            val chargeableWeight = max(weight, dimWeight)

            // Base rate + category surcharge
            val baseRate = rates[selectedService] ?: 60.0
            val categorySurcharge = when (selectedCategory) {
                "fragile"  -> 50.0
                "document" -> 0.0
                else       -> 0.0
            }
            val intlMultiplier = if (isInternational) 5.0 else 1.0
            val totalFee = (baseRate * chargeableWeight + categorySurcharge) * intlMultiplier

            // Show result card
            val resultCard = view.findViewById<CardView>(R.id.card_result)
            resultCard.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tv_shipping_fee).text =
                "₱ ${String.format("%.2f", totalFee)}"
            view.findViewById<TextView>(R.id.tv_est_delivery).text =
                delivery[selectedService] ?: "2-3 days"
            view.findViewById<TextView>(R.id.tv_chargeable_weight).text = "$chargeableWeight KG"

            Toast.makeText(requireContext(), "Rate calculated!", Toast.LENGTH_SHORT).show()
        }
    }
}