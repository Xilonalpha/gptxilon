package com.example.aiassistant

object ConversationEngine {
    fun canHandle(input:String)=DialogueEngine.canHandle(input)
    fun answer(input:String)=DialogueEngine.answer(input) ?: "Te ascult."
}
