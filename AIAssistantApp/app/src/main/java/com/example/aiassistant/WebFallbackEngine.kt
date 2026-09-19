package com.example.aiassistant

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.LinkedHashSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WebFallbackEngine(private val activity: Activity) {

    data class Result(
        val answer: String,
        val evidence: List<WebEvidence>,
        val searched: Boolean,
        val enginesTried: List<String>
    )

    private data class SearchHit(
        val title: String,
        val url: String,
        val snippet: String,
        val engine: String
    )

    private val handler = Handler(Looper.getMainLooper())

    fun ask(query: String): Result {
        val q = query.trim()
        if (q.isBlank()) {
            return Result("Nu există o întrebare de căutat.", emptyList(), false, emptyList())
        }

        val engines = listOf(
            "Google" to "https://www.google.com/search?q=",
            "Bing" to "https://www.bing.com/search?q=",
            "DuckDuckGo" to "https://html.duckduckgo.com/html/?q="
        )

        val hits = mutableListOf<SearchHit>()
        val tried = mutableListOf<String>()

        for ((name, base) in engines) {
            tried += name
            val page = loadPage(base + Uri.encode(q), 18_000L)
            if (page.isBlank()) continue

            hits += parseSearchPage(page, name)
            if (hits.size >= 10) break
        }

        val seen = LinkedHashSet<String>()
        val selected = hits.filter {
            seen.add(it.url.substringBefore("#").trimEnd('/'))
        }.take(6)

        if (selected.isEmpty()) {
            return Result(
                "Web Fallback nu a obținut rezultate publice. " +
                    "Motorul poate cere CAPTCHA, autentificare sau poate bloca WebView.",
                emptyList(),
                true,
                tried
            )
        }

        val evidence = mutableListOf<WebEvidence>()

        for (hit in selected) {
            val page = loadPage(hit.url, 15_000L)
            val text = extractReadableText(page)

            if (text.length >= 120 || hit.snippet.isNotBlank()) {
                evidence += WebEvidence(
                    title = hit.title.ifBlank { hit.url },
                    url = hit.url,
                    snippet = hit.snippet,
                    content = text.take(12_000),
                    source = hit.engine
                )
            }
        }

        return Result(buildAnswer(q, evidence), evidence, true, tried)
    }

    private fun loadPage(url: String, timeoutMs: Long): String {
        val latch = CountDownLatch(1)
        var result = ""

        handler.post {
            val webView = WebView(activity)
            configure(webView)

            var done = false

            fun finish(value: String) {
                if (done) return
                done = true
                result = value
                try {
                    webView.stopLoading()
                    webView.destroy()
                } catch (_: Exception) {
                }
                latch.countDown()
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view == null) {
                        finish("")
                        return
                    }

                    handler.postDelayed({
                        if (done) return@postDelayed

                        val js = """
                            (function() {
                                try {
                                    return JSON.stringify({
                                        title: document.title || "",
                                        url: location.href || "",
                                        html: document.documentElement
                                            ? document.documentElement.outerHTML : "",
                                        text: document.body ? document.body.innerText : ""
                                    });
                                } catch(e) {
                                    return JSON.stringify({
                                        title: "",
                                        url: location.href || "",
                                        html: "",
                                        text: ""
                                    });
                                }
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(js) { raw ->
                            finish(decodeJavascriptString(raw))
                        }
                    }, 900L)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) finish("")
                }
            }

            webView.loadUrl(url)
        }

        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return result
    }

    private fun configure(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.loadsImagesAutomatically = false
        webView.settings.blockNetworkImage = true
        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"
        CookieManager.getInstance().setAcceptCookie(true)
    }

    private fun parseSearchPage(raw: String, engine: String): List<SearchHit> {
        val html = jsonField(raw, "html")
        val text = jsonField(raw, "text")
        val result = mutableListOf<SearchHit>()

        val anchors = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        for (m in anchors.findAll(html).take(180)) {
            var href = decodeHtml(m.groupValues[1])
            val title = cleanText(m.groupValues[2])

            href = when {
                href.startsWith("/url?q=") ->
                    Uri.parse("https://www.google.com$href")
                        .getQueryParameter("q").orEmpty()

                href.startsWith("https://www.google.com/url?") ->
                    Uri.parse(href).getQueryParameter("q").orEmpty()

                href.startsWith("/l/?uddg=") ->
                    Uri.parse("https://html.duckduckgo.com$href")
                        .getQueryParameter("uddg").orEmpty()

                else -> href
            }

            if (!isUsefulResultUrl(href) || title.length < 3) continue

            result += SearchHit(title, href, "", engine)
            if (result.size >= 10) break
        }

        if (result.isEmpty() && text.length > 120) {
            result += SearchHit(
                "$engine — rezultate live",
                when (engine) {
                    "Google" -> "https://www.google.com/"
                    "Bing" -> "https://www.bing.com/"
                    else -> "https://duckduckgo.com/"
                },
                cleanText(text).take(2500),
                engine
            )
        }

        return result
    }

    private fun extractReadableText(raw: String): String {
        if (raw.isBlank()) return ""

        return jsonField(raw, "text")
            .replace('\u00A0', ' ')
            .lines()
            .map(::cleanText)
            .filter { it.length >= 25 }
            .filterNot {
                it.lowercase() in setOf(
                    "accept", "agree", "menu", "sign in", "log in",
                    "privacy", "terms", "cookie settings", "advertisement"
                )
            }
            .distinct()
            .joinToString("\n")
    }

    private fun buildAnswer(query: String, evidence: List<WebEvidence>): String {
        if (evidence.isEmpty()) {
            return "Nu am obținut conținut web utilizabil pentru: $query"
        }

        return buildString {
            append("🌐 WEB FALLBACK LIVE\n\n")
            append("Căutare: ").append(query).append('\n')
            append("Surse publice analizate: ").append(evidence.size).append("\n\n")

            evidence.forEachIndexed { i, item ->
                append("[").append(i + 1).append("] ")
                    .append(item.title).append('\n')
                append(item.url).append('\n')
                append(item.content.ifBlank { item.snippet }.take(1800))
                    .append("\n\n")
            }

            append("⚠️ Conținut colectat live prin WebView. ")
            append("Unele pagini pot fi incomplete sau pot bloca automatizarea.")
        }
    }

    private fun jsonField(raw: String, field: String): String {
        val key = "\"$field\":"
        val start = raw.indexOf(key)
        if (start < 0) return ""

        var i = start + key.length
        while (i < raw.length && raw[i].isWhitespace()) i++
        if (i >= raw.length || raw[i] != '"') return ""

        i++
        val out = StringBuilder()
        var escaped = false

        while (i < raw.length) {
            val c = raw[i++]

            if (escaped) {
                out.append(
                    when (c) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        '"' -> '"'
                        '\\' -> '\\'
                        '/' -> '/'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        else -> c
                    }
                )
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                break
            } else {
                out.append(c)
            }
        }

        return out.toString()
    }

    private fun decodeJavascriptString(raw: String): String =
        if (raw == "null") "" else jsonField("""{"value":$raw}""", "value")

    private fun cleanText(value: String): String =
        decodeHtml(value)
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun decodeHtml(value: String): String =
        value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")

    private fun isUsefulResultUrl(url: String): Boolean {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return false

        val host = Uri.parse(url).host?.lowercase().orEmpty()
        if (host.isBlank()) return false

        return host !in setOf(
            "google.com", "www.google.com",
            "bing.com", "www.bing.com",
            "duckduckgo.com", "www.duckduckgo.com",
            "html.duckduckgo.com"
        )
    }
}
