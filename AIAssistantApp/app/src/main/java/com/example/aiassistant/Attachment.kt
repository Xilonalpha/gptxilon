package com.example.aiassistant

data class Attachment(val name: String, val mime: String, val bytes: ByteArray? = null, val text: String? = null)
