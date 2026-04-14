package com.jtexpress.app

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = ""
)