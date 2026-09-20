package com.example.aiassistant

object ConfidenceEngine {
    fun score(intent: IntentEngine.Result, sourceConfidence: Int, verification: VerificationEngine.Result, memoryHit: Boolean): Int {
        var score = (sourceConfidence * 0.65 + intent.confidence * 0.2 + verification.score * 0.15).toInt()
        if (memoryHit) score += 3
        return score.coerceIn(5, 98)
    }
}
