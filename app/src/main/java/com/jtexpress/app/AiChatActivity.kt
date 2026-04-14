package com.jtexpress.app

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AiChatActivity : AppCompatActivity() {

    // ── AI API Config ─────────────────────────────────────────────
    // PRIMARY: Groq — get free key at https://console.groq.com
    private val GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions"
    private val GROQ_KEY   = "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"
    private val GROQ_MODEL = "llama-3.3-70b-versatile"

    // BACKUP: OpenRouter — get free key at https://openrouter.ai
    private val OPENROUTER_URL   = "https://openrouter.ai/api/v1/chat/completions"
    private val OPENROUTER_KEY   = "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"
    private val OPENROUTER_MODEL = "mistralai/mistral-7b-instruct:free"

    // Firebase
    private val db = FirebaseFirestore.getInstance()

    // UI
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var cardTyping: CardView
    private lateinit var scrollSuggestions: View
    private lateinit var suggestionsContainer: LinearLayout

    // Chat
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    /**
     * Conversation history in OpenAI format.
     * Each item: { "role": "user"|"assistant", "content": "..." }
     */
    private val conversationHistory = mutableListOf<JSONObject>()

    // User data
    private var userName    = ""
    private var userEmail   = ""
    private var userUid     = ""
    private var userContext = ""

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val suggestions = listOf(
        "📦 How many active shipments do I have?",
        "💰 What's my total COD earnings?",
        "🚚 Where is my latest package?",
        "📊 Show my delivery summary",
        "⏱️ Any delayed shipments?",
        "📅 Shipments this month"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        val prefs: SharedPreferences = getSharedPreferences("JTExpressPrefs", MODE_PRIVATE)
        userEmail = prefs.getString("user_email", "") ?: ""
        userName  = prefs.getString("user_name",  "") ?: ""
        userUid   = prefs.getString("user_uid",   "") ?: ""

        initViews()
        setupRecyclerView()
        setupSuggestionChips()
        setupClickListeners()
        loadUserDataAndGreet()
    }

    // ── UI Setup ──────────────────────────────────────────────────

    private fun initViews() {
        rvChat               = findViewById(R.id.rv_chat)
        etMessage            = findViewById(R.id.et_message)
        btnSend              = findViewById(R.id.btn_send)
        btnBack              = findViewById(R.id.btn_back)
        btnClear             = findViewById(R.id.btn_clear_chat)
        cardTyping           = findViewById(R.id.card_typing)
        scrollSuggestions    = findViewById(R.id.scroll_suggestions)
        suggestionsContainer = findViewById(R.id.suggestions_container)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        rvChat.adapter = adapter
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun setupSuggestionChips() {
        suggestions.forEach { suggestion ->
            val chip = Chip(this).apply {
                text = suggestion
                isCheckable = false
                setChipBackgroundColorResource(R.color.jt_white)
                setTextColor(ContextCompat.getColor(context, R.color.jt_black))
                chipStrokeWidth = 1.5f
                setChipStrokeColorResource(R.color.jt_red)
                textSize = 12f
                setOnClickListener {
                    val query = suggestion.substring(2).trim()
                    etMessage.setText(query)
                    sendMessage()
                }
            }
            suggestionsContainer.addView(chip)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnSend.setOnClickListener { sendMessage() }
        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Start a fresh conversation?")
                .setPositiveButton("Clear") { _, _ ->
                    adapter.clearMessages()
                    conversationHistory.clear()
                    scrollSuggestions.visibility = View.VISIBLE
                    greetUser()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ── Load Firestore Data ───────────────────────────────────────

    private fun loadUserDataAndGreet() {
        if (userUid.isEmpty()) {
            userContext = "No shipment data available."
            greetUser()
            return
        }

        db.collection("users").document(userUid).get()
            .addOnSuccessListener { userDoc ->
                val phone = userDoc?.getString("phone") ?: "N/A"
                val role  = userDoc?.getString("role")  ?: "customer"

                db.collection("shipments")
                    .whereEqualTo("userId", userUid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .addOnSuccessListener { shipments ->
                        val sb  = StringBuilder()
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                        sb.appendLine("=== USER PROFILE ===")
                        sb.appendLine("Name: $userName | Email: $userEmail | Phone: $phone | Role: $role")
                        sb.appendLine()
                        sb.appendLine("=== SHIPMENTS ===")

                        var totalCOD  = 0.0
                        var delivered = 0; var inTransit = 0
                        var pending   = 0; var failed    = 0

                        for (doc in shipments.documents) {
                            val status      = doc.getString("status") ?: "Unknown"
                            val codAmount   = doc.getDouble("codAmount") ?: 0.0
                            val trackingNo  = doc.getString("trackingNumber") ?: doc.id
                            val from        = doc.getString("senderCity") ?: "N/A"
                            val to          = doc.getString("recipientCity") ?: "N/A"
                            val recipient   = doc.getString("recipientName") ?: "N/A"
                            val description = doc.getString("itemDescription") ?: "N/A"
                            val weight      = doc.getDouble("weight") ?: 0.0
                            val shippingFee = doc.getDouble("shippingFee") ?: 0.0
                            val createdAt   = doc.getTimestamp("createdAt")?.toDate()
                            val deliveredAt = doc.getTimestamp("deliveredAt")?.toDate()

                            when (status.lowercase()) {
                                "delivered"                -> { delivered++; totalCOD += codAmount }
                                "in transit", "in_transit" -> inTransit++
                                "pending"                  -> pending++
                                "failed", "returned"       -> failed++
                            }

                            sb.append("[$trackingNo] $status | $from→$to | ")
                            sb.append("$recipient | $description | ${weight}kg | ")
                            sb.append("COD:₱$codAmount | Fee:₱$shippingFee")
                            if (createdAt != null)   sb.append(" | Created:${sdf.format(createdAt)}")
                            if (deliveredAt != null) sb.append(" | Delivered:${sdf.format(deliveredAt)}")
                            sb.appendLine()
                        }

                        sb.appendLine()
                        sb.appendLine("=== SUMMARY ===")
                        sb.appendLine("Total:${shipments.size()} Delivered:$delivered InTransit:$inTransit Pending:$pending Failed:$failed TotalCOD:₱${"%.2f".format(totalCOD)}")

                        userContext = sb.toString()
                        greetUser()
                    }
                    .addOnFailureListener {
                        userContext = "Shipment data unavailable."
                        greetUser()
                    }
            }
            .addOnFailureListener {
                userContext = "User data unavailable."
                greetUser()
            }
    }

    // ── Greeting ──────────────────────────────────────────────────

    private fun greetUser() {
        val hour      = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting  = when { hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" }
        val firstName = userName.split(" ").firstOrNull()?.ifEmpty { "there" } ?: "there"
        val msg = "$greeting, $firstName! 👋\n\nI'm your J&T Express AI Assistant. I can help you with:\n\n• 📦 Tracking your shipments\n• 💰 Checking your COD earnings\n• 📊 Viewing your delivery stats\n• 🚚 Finding delayed or pending packages\n\nWhat would you like to know?"
        adapter.addMessage(ChatMessage(msg, isUser = false, timestamp = currentTime()))
        scrollToBottom()
    }

    // ── Send Message ──────────────────────────────────────────────

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        scrollSuggestions.visibility = View.GONE
        etMessage.setText("")
        adapter.addMessage(ChatMessage(text, isUser = true, timestamp = currentTime()))
        scrollToBottom()
        showTyping(true)
        callAI(text)
    }

    // ── AI API (Groq primary, OpenRouter backup) ──────────────────

    private fun callAI(userMessage: String) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Try Groq first
                    var response = callOpenAICompatible(
                        url     = GROQ_URL,
                        key     = GROQ_KEY,
                        model   = GROQ_MODEL,
                        userMessage = userMessage
                    )

                    // If Groq hits rate limit or fails, fallback to OpenRouter
                    if (response.first == 429 || response.first >= 500) {
                        response = callOpenAICompatible(
                            url     = OPENROUTER_URL,
                            key     = OPENROUTER_KEY,
                            model   = OPENROUTER_MODEL,
                            userMessage = userMessage
                        )
                    }
                    response
                }

                showTyping(false)
                val (code, body) = result

                if (code == 200) {
                    val replyText = JSONObject(body)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    // Save to history in OpenAI format
                    conversationHistory.add(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                    conversationHistory.add(JSONObject().apply {
                        put("role", "assistant")
                        put("content", replyText)
                    })

                    adapter.addMessage(ChatMessage(replyText, isUser = false, timestamp = currentTime()))
                    scrollToBottom()

                } else {
                    val errMsg = try {
                        val err = JSONObject(body).optJSONObject("error")
                        val msg = err?.optString("message") ?: ""
                        when (code) {
                            400  -> "Invalid request: $msg"
                            401  -> "Invalid API key. Check your Groq/OpenRouter keys."
                            403  -> "API key lacks permission."
                            429  -> "Both AI services are busy. Please wait a moment and try again."
                            500  -> "AI server error. Try again shortly."
                            else -> "Error $code: $msg"
                        }
                    } catch (e: Exception) {
                        "Error $code — check your API keys and internet connection."
                    }
                    showError(errMsg)
                }

            } catch (e: Exception) {
                showTyping(false)
                showError("Network error: ${e.message ?: "Check your connection."}")
            }
        }
    }

    private fun callOpenAICompatible(
        url: String,
        key: String,
        model: String,
        userMessage: String
    ): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.doOutput      = true
        conn.connectTimeout = 30_000
        conn.readTimeout    = 30_000

        // Build messages: system prompt + history + new user message
        val messages = JSONArray()

        // System prompt
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", buildSystemPrompt())
        })

        // Previous conversation turns
        conversationHistory.forEach { messages.put(it) }

        // Current user message
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("max_tokens", 1024)
            put("temperature", 0.7)
        }

        OutputStreamWriter(conn.outputStream).use { w ->
            w.write(body.toString())
            w.flush()
        }

        val code   = conn.responseCode
        val stream = if (code == 200) conn.inputStream else conn.errorStream
        val text   = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        conn.disconnect()

        return Pair(code, text)
    }

    // ── System Prompt ─────────────────────────────────────────────

    private fun buildSystemPrompt(): String {
        val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        return """
You are J&T Express AI Assistant built into the J&T Express Philippines mobile app.
Be friendly, professional, and concise. Use bullet points when listing items.

Today's date: $today
Currency: Philippine Peso (₱) — always include the ₱ symbol.

REAL USER DATA (answer based on this only — never invent data):
$userContext

RULES:
- If a tracking number is not in the data above, say it was not found.
- If no shipments are listed, acknowledge kindly and suggest checking back later.
- You can calculate totals, averages, and counts from the data above.
        """.trimIndent()
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun showTyping(show: Boolean) {
        cardTyping.visibility = if (show) View.VISIBLE else View.GONE
        if (show) scrollToBottom()
    }

    private fun showError(msg: String) {
        adapter.addMessage(ChatMessage("⚠️ $msg", isUser = false, timestamp = currentTime()))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (adapter.itemCount > 0)
            rvChat.post { rvChat.smoothScrollToPosition(adapter.itemCount - 1) }
    }

    private fun currentTime(): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}