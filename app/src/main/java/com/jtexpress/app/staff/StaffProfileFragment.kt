package com.jtexpress.app.staff

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.jtexpress.app.R
import com.jtexpress.app.StaffMainActivity

class StaffProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_staff_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("JTExpressPrefs", Context.MODE_PRIVATE)
        view.findViewById<TextView>(R.id.tv_staff_name)?.text       = prefs.getString("user_name", "Staff")
        view.findViewById<TextView>(R.id.tv_staff_email)?.text      = prefs.getString("user_email", "")
        view.findViewById<TextView>(R.id.tv_staff_role_label)?.text = "Branch Staff"

        view.findViewById<MaterialButton>(R.id.btn_staff_logout)
            ?.setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Log Out") { _, _ ->
                        (activity as? StaffMainActivity)?.logout()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
    }
}