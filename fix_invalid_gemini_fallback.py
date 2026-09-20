#!/usr/bin/env python3
from pathlib import Path

ROOT = Path.home() / "gptxilon"
MAIN = ROOT / "AIAssistantApp/app/src/main/java/com/example/aiassistant/MainActivity.kt"
GEMINI = ROOT / "AIAssistantApp/app/src/main/java/com/example/aiassistant/GeminiClient.kt"

if not MAIN.exists():
    raise SystemExit(f"Nu găsesc: {MAIN}")
if not GEMINI.exists():
    raise SystemExit(f"Nu găsesc: {GEMINI}")

gem = GEMINI.read_text()
main = MAIN.read_text()

old_throw = '            if (!response.isSuccessful) throw Exception("Eroare API ${response.code}: ${raw.take(300)}")'
new_throw = '            if (!response.isSuccessful) throw GeminiApiException(response.code, raw)'

if old_throw not in gem:
    if "throw GeminiApiException(response.code, raw)" not in gem:
        raise SystemExit("GeminiClient.kt: nu am găsit punctul de reparat. Nu am modificat nimic.")

old_catch = """                    } catch (e: Exception) {
                        val message=e.message.orEmpty()

                        if (message.startsWith("Eroare API 401")) {
                            secureStore.clear()
                            apiKey=""

                            runOnUiThread {
                                binding.etApiKey.setText("")
                                toast("⚠️ Cheia Gemini este invalidă. Am trecut automat pe motorul local.")
                            }

                            AiResponse(
                                OfflineBrain.reply(text),
                                emptyList(),
                                0
                            )
                        } else {
                            throw e
                        }
                    }"""

new_catch = """                    } catch (e: Exception) {
                        if (e is GeminiApiException && e.isAuthenticationFailure()) {
                            secureStore.clear()
                            apiKey=""

                            runOnUiThread {
                                binding.etApiKey.setText("")
                                toast("⚠️ Cheia Gemini nu este validă/acceptată. Am trecut automat pe motorul local.")
                            }

                            if (plan.useWeb) {
                                val report = researchEngine.research(text, plan.deep)
                                collectedEvidence.addAll(report.evidence)

                                AiResponse(
                                    report.answer,
                                    report.evidence.map { "${it.title} — ${it.url}" },
                                    0
                                )
                            } else {
                                AiResponse(
                                    OfflineBrain.reply(text),
                                    emptyList(),
                                    0
                                )
                            }
                        } else {
                            throw e
                        }
                    }"""

if old_catch not in main:
    if "e is GeminiApiException && e.isAuthenticationFailure()" not in main:
        raise SystemExit("MainActivity.kt: fallback-ul vechi nu a fost găsit. Nu am modificat nimic.")

# Prepare both files completely before writing either one.
if old_throw in gem:
    if "class GeminiApiException(" not in gem:
        marker = "class GeminiClient(private val apiKey: String) {"
        exception = """class GeminiApiException(
    val code: Int,
    val rawBody: String
) : Exception("Eroare API " + code + ": " + rawBody.take(300)) {

    fun isAuthenticationFailure(): Boolean {
        val body = rawBody.lowercase()
        val invalidKey =
            body.contains("api_key_invalid") ||
            body.contains("api key not valid") ||
            body.contains("invalid api key") ||
            (body.contains("invalid_argument") && body.contains("api key")) ||
            (body.contains("expired") && body.contains("key")) ||
            (body.contains("revoked") && body.contains("key"))

        val blockedKey =
            body.contains("api_key_service_blocked") ||
            (body.contains("api key") && body.contains("permission_denied"))

        return code == 401 ||
            (code == 400 && invalidKey) ||
            (code == 403 && (invalidKey || blockedKey))
    }
}

class GeminiClient(private val apiKey: String) {"""
        if marker not in gem:
            raise SystemExit("GeminiClient.kt: markerul clasei nu a fost găsit. Nu am modificat nimic.")
        gem = gem.replace(marker, exception, 1)
    gem = gem.replace(old_throw, new_throw, 1)

if old_catch in main:
    main = main.replace(old_catch, new_catch, 1)

# Atomic-ish writes: keep backups, then write.
GEMINI.with_suffix(".kt.bak").write_text(GEMINI.read_text(), encoding="utf-8")
MAIN.with_suffix(".kt.bak").write_text(MAIN.read_text(), encoding="utf-8")
GEMINI.write_text(gem, encoding="utf-8")
MAIN.write_text(main, encoding="utf-8")

print("OK: fallback-ul pentru cheia Gemini invalidă/expirată a fost reparat.")
print(" - GeminiClient.kt: identifică explicit 400 API_KEY_INVALID, 401 și cazuri 403 de cheie blocată.")
print(" - MainActivity.kt: șterge cheia invalidă și trece automat la motorul local.")
print(" - Dacă întrebarea necesită web, motorul local folosește ResearchEngine/WebFallback, nu doar OfflineBrain.")
print("Backup-uri create: GeminiClient.kt.bak și MainActivity.kt.bak")
