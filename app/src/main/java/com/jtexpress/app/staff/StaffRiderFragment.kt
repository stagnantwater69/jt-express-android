package com.jtexpress.app.staff

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.jtexpress.app.R

class StaffRiderFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_staff_riders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.riders_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_no_riders)

        db.collection("users")
            .whereEqualTo("role", "rider")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                if (snapshot.isEmpty) {
                    tvEmpty?.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                tvEmpty?.visibility = View.GONE

                snapshot.documents.forEach { doc ->
                    val name     = "${doc.getString("firstName")} ${doc.getString("lastName")}"
                    val isActive = doc.getBoolean("isActive") ?: false
                    val dp       = resources.displayMetrics.density

                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        val p = (14 * dp).toInt()
                        setPadding(p, p, p, p)
                        setBackgroundColor(Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = (1 * dp).toInt() }
                    }

                    row.addView(TextView(requireContext()).apply {
                        text = name; textSize = 13f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_black))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })

                    row.addView(TextView(requireContext()).apply {
                        text = if (isActive) "Active" else "Inactive"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(requireContext(),
                            if (isActive) android.R.color.holo_green_dark else R.color.jt_gray))
                    })

                    container?.addView(row)
                }
            }
    }
}