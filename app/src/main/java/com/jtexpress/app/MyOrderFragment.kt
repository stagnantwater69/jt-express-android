package com.jtexpress.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class MyOrderFragment : Fragment() {

    private var selectedTab = "all"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_order, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs(view)
        setupSearch(view)
        setupOrderButtons(view)
    }

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

                // Update indicator underline
                indicators.forEach { (k, indicator) ->
                    indicator.setBackgroundColor(
                        if (k == key) resources.getColor(R.color.jt_red, null)
                        else android.graphics.Color.TRANSPARENT
                    )
                }

                // Update all TextViews inside every tab layout (count + label)
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

                filterOrders(view, key)
            }
        }
    }

    private fun filterOrders(view: View, tab: String) {
        val order1 = view.findViewById<View>(R.id.order_1)
        val order2 = view.findViewById<View>(R.id.order_2)
        val order3 = view.findViewById<View>(R.id.order_3)

        when (tab) {
            "all" -> {
                order1.visibility = View.VISIBLE
                order2.visibility = View.VISIBLE
                order3.visibility = View.VISIBLE
            }
            "send" -> {
                order1.visibility = View.VISIBLE
                order2.visibility = View.GONE
                order3.visibility = View.VISIBLE
            }
            "receive" -> {
                order1.visibility = View.GONE
                order2.visibility = View.VISIBLE
                order3.visibility = View.GONE
            }
            "pending" -> {
                order1.visibility = View.GONE
                order2.visibility = View.GONE
                order3.visibility = View.VISIBLE
            }
        }
    }

    private fun setupSearch(view: View) {
        view.findViewById<Button>(R.id.btn_search).setOnClickListener {
            val query = view.findViewById<TextInputEditText>(R.id.et_search)
                .text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a tracking number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "Searching: $query", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupOrderButtons(view: View) {
        // Track buttons
        view.findViewById<Button>(R.id.btn_track_1).setOnClickListener {
            Toast.makeText(requireContext(), "Tracking JT2024001234PH…", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.btn_track_2).setOnClickListener {
            Toast.makeText(requireContext(), "Tracking JT2024000891PH…", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.btn_track_3).setOnClickListener {
            Toast.makeText(requireContext(), "Tracking JT2024001456PH…", Toast.LENGTH_SHORT).show()
        }

        // Copy buttons
        view.findViewById<Button>(R.id.btn_copy_1).setOnClickListener {
            copyToClipboard("JT2024001234PH")
        }
        view.findViewById<Button>(R.id.btn_copy_2).setOnClickListener {
            copyToClipboard("JT2024000891PH")
        }
        view.findViewById<Button>(R.id.btn_copy_3).setOnClickListener {
            copyToClipboard("JT2024001456PH")
        }
    }

    private fun copyToClipboard(trackingNo: String) {
        val clipboard = requireContext()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Tracking Number", trackingNo))
        Toast.makeText(requireContext(), "Copied: $trackingNo", Toast.LENGTH_SHORT).show()
    }
}