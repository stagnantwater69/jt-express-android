package com.jtexpress.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class AdminAuditFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_audit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.audit_log_container)
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        db.collection("auditLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(200)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val dp = resources.displayMetrics.density

                snapshot.documents.forEach { doc ->
                    val action   = doc.getString("action") ?: "unknown"
                    val actorUid = doc.getString("actorUid") ?: "—"
                    val dateStr  = doc.getTimestamp("timestamp")?.toDate()
                        ?.let { sdf.format(it) } ?: "—"

                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        val p = (12 * dp).toInt()
                        setPadding(p, p, p, p)
                        setBackgroundColor(Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = (1 * dp).toInt() }
                    }

                    row.addView(TextView(requireContext()).apply {
                        text = action.replace("_", " ").uppercase()
                        textSize = 12f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_black))
                    })
                    row.addView(TextView(requireContext()).apply {
                        text = "By: $actorUid  •  $dateStr"
                        textSize = 10f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.jt_gray))
                    })

                    container?.addView(row)
                }
            }
    }
}
