package com.jtexpress.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData(view)
        setGreeting(view)
        setupClickListeners(view)
    }

    private fun loadUserData(view: View) {
        // User data loaded via SharedPreferences if needed in future
    }

    private fun setGreeting(view: View) {
        // Greeting label removed from new layout — no-op
    }

    private fun setupClickListeners(view: View) {

        // --- Scanner button
        view.findViewById<View>(R.id.btn_scanner)?.setOnClickListener {
            Toast.makeText(requireContext(), "Scanner — Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // --- Notification bell
        view.findViewById<View>(R.id.btn_notification)?.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
        }

        // --- Order card → My Order tab
        view.findViewById<CardView>(R.id.card_order)?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }

        // --- Tracking card → opens quick track input
        view.findViewById<CardView>(R.id.card_tracking)?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }

        // --- Services grid
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

        // --- Active shipment → view details
        view.findViewById<MaterialButton>(R.id.btn_view_details)?.setOnClickListener {
            Toast.makeText(requireContext(), "Opening shipment details...", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(requireContext(), TrackingDetailActivity::class.java))
        }

        // --- See all recent deliveries → My Order tab
        view.findViewById<TextView>(R.id.tv_see_all)?.setOnClickListener {
            (activity as? MainActivity)?.switchToTab(R.id.nav_my_order)
        }

        // --- Hotline → dial J&T hotline
        view.findViewById<CardView>(R.id.card_hotline)?.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+63288911818")
            }
            startActivity(dialIntent)
        }
    }
}