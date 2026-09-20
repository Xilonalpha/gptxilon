package com.example.aiassistant

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(private val apiKey: String) {
    companion object {
        val PERSONAS = listOf(
            "Asistent Universal" to "Ești un asistent AI universal, echilibrat și direct.",
            "Mentor" to "Ești un mentor pedagogic: explici clar, cu exemple utile.",
            "Dezvoltator Senior" to "Ești un dezvoltator software senior: cod complet, precis și verificabil.",
            "Cercetător" to "Ești un cercetător riguros: separi dovezile de ipoteze și citezi sursele.",
            "Analist Critic" to "Ești un analist critic: cauți contradicții, lipsuri și presupuneri ascunse."
        )
        fun personaPrompt(index: Int) = PERSONAS.getOrElse(index) { PERSONAS[0] }.second
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun ask(history: List<ChatMessage>, brainContext: String = "", personaIndex: Int = 0,
            thinkingMode: Boolean = false, forceSearch: Boolean = true,
            agentMode: Boolean = false, graphContext: String = ""): AiResponse {
        return askInternal(history, brainContext, personaIndex, thinkingMode, forceSearch, agentMode, graphContext, null)
    }

    fun askWithAttachment(history: List<ChatMessage>, attachment: Attachment,
                          brainContext: String = "", personaIndex: Int = 0,
                          forceSearch: Boolean = false, graphContext: String = ""): AiResponse {
        return askInternal(history, brainContext, personaIndex, false, forceSearch, false, graphContext, attachment)
    }

    private fun askInternal(history: List<ChatMessage>, brainContext: String, personaIndex: Int,
                            thinkingMode: Boolean, forceSearch: Boolean, agentMode: Boolean,
                            graphContext: String, attachment: Attachment?): AiResponse {
        val contents = JSONArray()
        history.takeLast(24).forEach { msg ->
            contents.put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", msg.text))).put("role", if (msg.isUser) "user" else "model"))
        }
        if (attachment != null && history.isNotEmpty()) {
            val last = contents.optJSONObject(contents.length() - 1)
            if (last != null && last.optString("role") == "user") {
                val parts = last.getJSONArray("parts")
                if (attachment.text != null) {
                    parts.put(JSONObject().put("text", "\n\nFIȘIER ATAȘAT: ${attachment.name}\n${attachment.text}"))
                } else if (attachment.bytes != null) {
                    parts.put(JSONObject().put("inlineData", JSONObject()
                        .put("mimeType", attachment.mime)
                        .put("data", Base64.encodeToString(attachment.bytes, Base64.NO_WRAP))))
                }
            }
        }

        val systemText = buildString {
            append(personaPrompt(personaIndex)).append(" ")
            append("Răspunde în limba utilizatorului. Nu inventa fapte. ")
            append("Separă clar informația confirmată, inferența și necunoscutul. ")
            append("Pentru informații actuale folosește grounding-ul web și bazează afirmațiile actuale pe sursele găsite. ")
            append("Nu afișa chain-of-thought sau raționament intern privat. ")
            if (thinkingMode) append("Oferă un REZUMAT AL ABORDĂRII în maximum 4 pași, apoi răspunsul final. ")
            if (agentMode) append(AgentEngine.instruction(history.lastOrNull()?.text ?: ""))
            if (brainContext.isNotBlank()) append(" Context memorie: ").append(brainContext)
            if (graphContext.isNotBlank()) append(" Knowledge graph relevant: ").append(graphContext.take(6000))
        }

        val body = JSONObject()
            .put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemText))))
            .put("generationConfig", JSONObject().put("temperature", 0.15).put("topP", 0.9).put("maxOutputTokens", 12288))
        if (forceSearch) body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Eroare API ${response.code}: ${raw.take(300)}")
            val json = JSONObject(raw)
            val candidates = json.optJSONArray("candidates") ?: throw Exception("Răspuns Gemini fără candidates")
            if (candidates.length() == 0) throw Exception("Gemini nu a returnat un răspuns")
            val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
            val text = buildString { for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text")) }.trim()
            val sources = mutableListOf<String>()
            json.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")?.let { chunks ->
                for (i in 0 until chunks.length()) chunks.optJSONObject(i)?.optJSONObject("web")?.let { w ->
                    val title = w.optString("title"); val uri = w.optString("uri")
                    if (title.isNotBlank()) sources.add(if (uri.isNotBlank()) "$title — $uri" else title)
                }
            }
            val tokens = json.optJSONObject("usageMetadata")?.optInt("totalTokenCount") ?: 0
            return AiResponse(text, sources.distinct(), tokens)
        }
    }
}
