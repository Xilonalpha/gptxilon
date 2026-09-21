package com.example.aiassistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val onPartial: (String) -> Unit = {},
    private val onFinal: (String) -> Unit,
    private val onState: (String) -> Unit = {}
) : RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    var voiceRepliesEnabled: Boolean = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                configureTts(Locale.getDefault())
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = createRecognizer()
        }
    }

    private fun createRecognizer(): SpeechRecognizer {
        val r =
            if (
                android.os.Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

        r.setRecognitionListener(this)
        return r
    }

    fun startListening(locale: Locale = Locale.getDefault()) {
        if (recognizer == null) {
            onState("🎤 Recunoașterea vocală nu este disponibilă")
            return
        }

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                locale.toLanguageTag()
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                locale.toLanguageTag()
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )
            putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.packageName
            )
        }

        try {
            recognizer?.startListening(intent)
            onState("🎤 Ascult…")
        } catch (e: Exception) {
            onState(
                "⚠️ Nu pot porni microfonul: ${e.message ?: "eroare"}"
            )
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun toggleVoiceReplies(): Boolean {
        voiceRepliesEnabled = !voiceRepliesEnabled

        if (!voiceRepliesEnabled) {
            stopSpeaking()
        }

        onState(
            if (voiceRepliesEnabled)
                "🔊 Răspuns vocal ACTIV"
            else
                "🔇 Răspuns vocal OPRIT"
        )

        return voiceRepliesEnabled
    }

    fun speak(text: String, locale: Locale = Locale.getDefault()) {
        if (!voiceRepliesEnabled) return
        speakNow(text, locale)
    }

    fun speakNow(text: String, locale: Locale = Locale.getDefault()) {
        if (!ttsReady || text.isBlank()) return

        val clean = cleanForSpeech(text)

        if (clean.isBlank()) return

        configureTts(locale)

        try {
            tts?.speak(
                clean.take(6000),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ai_reply"
            )

            onState("🔊 Redau răspunsul…")
        } catch (_: Exception) {
            onState("⚠️ Vocea nu a putut fi redată")
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }

        tts = null
    }

    private fun configureTts(locale: Locale) {
        val engine = tts ?: return

        val result = engine.setLanguage(locale)

        if (
            result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            engine.setLanguage(Locale.US)
        }

        engine.setSpeechRate(0.98f)
        engine.setPitch(1.0f)
    }

    private fun cleanForSpeech(value: String): String {
        return value
            .replace(Regex("\\[file:[^]]+\\]"), "")
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("[*_#>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        onState("🎤 Ascult…")
    }

    override fun onBeginningOfSpeech() {
        onState("🎤 Vorbește…")
    }

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        onState("🧠 Procesez vocea…")
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO ->
                "Eroare audio"

            SpeechRecognizer.ERROR_CLIENT ->
                "Eroare client"

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Permisiunea microfonului lipsește"

            SpeechRecognizer.ERROR_NETWORK ->
                "Eroare de rețea"

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Timeout rețea"

            SpeechRecognizer.ERROR_NO_MATCH ->
                "Nu am înțeles vocea"

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Recunoașterea este ocupată"

            SpeechRecognizer.ERROR_SERVER ->
                "Serviciul vocal a returnat o eroare"

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "Nu s-a detectat vorbire"

            else ->
                "Eroare vocală ($error)"
        }

        onState("⚠️ $message")
    }

    override fun onResults(results: Bundle?) {
        val text =
            results
                ?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

        if (text.isNotBlank()) {
            onState("✅ Am înțeles")
            onFinal(text)
        } else {
            onState("⚠️ Nu am primit text")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text =
            partialResults
                ?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

        if (text.isNotBlank()) {
            onPartial(text)
        }
    }

    override fun onEvent(
        eventType: Int,
        params: Bundle?
    ) = Unit
}
