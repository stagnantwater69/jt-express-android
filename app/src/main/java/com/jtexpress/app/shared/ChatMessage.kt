package com.jtexpress.app.shared

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = ""
)