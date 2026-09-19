package com.example.aiassistant

data class AiResponse(
    val text: String,
    val sources: List<String>,
    val tokens: Int = 0
)
