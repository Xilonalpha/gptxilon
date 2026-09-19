package com.example.aiassistant

data class WebEvidence(
    val title: String,
    val url: String,
    val snippet: String = "",
    val content: String = "",
    val source: String = ""
)
