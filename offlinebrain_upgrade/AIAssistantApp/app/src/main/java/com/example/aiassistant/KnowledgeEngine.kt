package com.example.aiassistant

object KnowledgeEngine {
    fun canHandle(input:String)=LocalKnowledgeBase.canHandle(input)
    fun answer(input:String)=LocalKnowledgeBase.answer(input) ?: "Nu am această informație în baza mea locală."
    fun domain(input:String)=LocalKnowledgeBase.domain(input)
    fun size()=LocalKnowledgeBase.size()
}
