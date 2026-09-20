AI Assistant — LIVE WEB FALLBACK
================================

These files are based on the real current files read from Xilonalpha/gptxilon.

Copy/overwrite:
AIAssistantApp/app/src/main/java/com/example/aiassistant/MainActivity.kt
AIAssistantApp/app/src/main/java/com/example/aiassistant/WebEvidence.kt
AIAssistantApp/app/src/main/java/com/example/aiassistant/WebFallbackEngine.kt
AIAssistantApp/app/src/main/AndroidManifest.xml

Manifest already had INTERNET in the real repository, so no new permission was needed.

Behavior:
- Gemini key exists -> existing Gemini path.
- Gemini key absent -> live WebView fallback.
- Search tries Google, Bing, DuckDuckGo.
- Public result pages are opened and rendered text is extracted.
- Sources are returned to the existing evidence system.

WebView is used instead of trying to control the external Chrome app because Android
does not expose Chrome's DOM/data to another application. Android documents WebView
loadUrl/evaluateJavascript for this type of embedded web access.

The fallback does not bypass CAPTCHA, login, paywalls or private content.
