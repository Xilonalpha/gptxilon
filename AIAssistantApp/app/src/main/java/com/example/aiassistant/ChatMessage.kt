package com.example.aiassistant

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val sources: List<String> = emptyList(),
    var feedbackRated: Boolean = false
)
