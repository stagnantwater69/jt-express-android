package com.jtexpress.app.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R

class AdminSettingsFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load current base rate
        db.collection("app_config").document("rates_config").get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val baseRate = doc?.getDouble("base_rate_per_kg") ?: 50.0
                view.findViewById<EditText>(R.id.et_base_rate)?.setText(baseRate.toString())
            }

        // Save rate
        view.findViewById<MaterialButton>(R.id.btn_save_rate)?.setOnClickListener {
            val newRate = view.findViewById<EditText>(R.id.et_base_rate)
                ?.text?.toString()?.toDoubleOrNull() ?: return@setOnClickListener
            db.collection("app_config").document("rates_config")
                .update("base_rate_per_kg", newRate)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Rate saved!", Toast.LENGTH_SHORT).show()
                }
        }

        // Broadcast message
        view.findViewById<MaterialButton>(R.id.btn_send_broadcast)?.setOnClickListener {
            val msg = view.findViewById<EditText>(R.id.et_broadcast_message)
                ?.text?.toString()?.trim() ?: return@setOnClickListener
            if (msg.isEmpty()) return@setOnClickListener

            db.collection("notifications").add(
                mapOf(
                    "message"   to msg,
                    "target"    to "all",
                    "createdAt" to Timestamp.now()
                )
            ).addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Broadcast sent!", Toast.LENGTH_SHORT).show()
                view.findViewById<EditText>(R.id.et_broadcast_message)?.setText("")
            }
        }

        // Logout
        view.findViewById<MaterialButton>(R.id.btn_admin_logout)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure?")
                .setPositiveButton("Log Out") { _, _ ->
                    (activity as? AdminMainActivity)?.logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}