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
            return Result(
                "Nu există o întrebare de căutat.",
                emptyList(),
                false,
                emptyList()
            )
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

            val page = loadPage(
                base + Uri.encode(q),
                18_000L
            )

            if (page.isBlank()) continue

            hits += parseSearchPage(page, name)

            if (hits.size >= 15) break
        }

        val seen = LinkedHashSet<String>()

        val selected = hits
            .filter {
                val normalized = normalizeUrl(it.url)
                normalized.isNotBlank() && seen.add(normalized)
            }
            .filter { isUsefulResultUrl(it.url) }
            .take(10)

        if (selected.isEmpty()) {
            return Result(
                "Motoarele de căutare au răspuns, dar extractorul " +
                    "nu a găsit URL-uri externe de articole/documente. " +
                    "Paginile Google/Bing/DuckDuckGo nu sunt tratate " +
                    "ca surse.",
                emptyList(),
                true,
                tried
            )
        }

        val evidence = mutableListOf<WebEvidence>()

        for (hit in selected) {
            val page = loadPage(
                hit.url,
                15_000L
            )

            if (page.isBlank()) continue

            val finalUrl = jsonField(page, "url")
                .ifBlank { hit.url }
                .trim()

            if (!isUsefulResultUrl(finalUrl)) continue

            val text = extractReadableText(page)

            if (text.length < 180) continue

            if (isNoiseContent(text, hit.title)) continue

            evidence += WebEvidence(
                title = hit.title.ifBlank { finalUrl },
                url = finalUrl,
                snippet = hit.snippet,
                content = text.take(12_000),
                source = hit.engine
            )
        }

        if (evidence.isEmpty()) {
            return Result(
                "Au fost găsite rezultate externe, dar paginile " +
                    "nu au furnizat suficient conținut public " +
                    "verificabil. Nu voi transforma paginile " +
                    "motoarelor de căutare în dovezi.",
                emptyList(),
                true,
                tried
            )
        }

        return Result(
            buildAnswer(q, evidence),
            evidence,
            true,
            tried
        )
    }

    private fun loadPage(
        url: String,
        timeoutMs: Long
    ): String {

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

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    if (view == null) {
                        finish("")
                        return
                    }

                    handler.postDelayed({

                        if (done) return@postDelayed

                        val js = """
                            (function() {
                                try {
                                    var links = [];

                                    document.querySelectorAll("a").forEach(function(a) {
                                        var href = a.href || "";
                                        var text = (a.innerText || a.textContent || "").trim();

                                        if (href && text) {
                                            links.push({
                                                href: href,
                                                text: text
                                            });
                                        }
                                    });

                                    return JSON.stringify({
                                        title: document.title || "",
                                        url: location.href || "",
                                        html: document.documentElement
                                            ? document.documentElement.outerHTML : "",
                                        text: document.body
                                            ? document.body.innerText : "",
                                        links: links
                                    });
                                } catch(e) {
                                    return JSON.stringify({
                                        title: "",
                                        url: location.href || "",
                                        html: "",
                                        text: "",
                                        links: []
                                    });
                                }
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(js) { raw ->
                            finish(decodeJavascriptString(raw))
                        }

                    }, 800L)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        finish("")
                    }
                }
            }

            webView.loadUrl(url)
        }

        latch.await(
            timeoutMs,
            TimeUnit.MILLISECONDS
        )

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

        CookieManager.getInstance()
            .setAcceptCookie(true)
    }

    private fun parseSearchPage(
        raw: String,
        engine: String
    ): List<SearchHit> {

        val result = mutableListOf<SearchHit>()

        val linksJson = extractJsonArray(
            raw,
            "links"
        )

        val linkPattern = Regex(
            """\{"href":"(.*?)","text":"(.*?)"\}""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        for (match in linkPattern.findAll(linksJson)) {

            var href = decodeEscaped(
                match.groupValues[1]
            )

            val title = cleanText(
                decodeEscaped(
                    match.groupValues[2]
                )
            )

            href = resolveSearchUrl(
                href,
                engine
            )

            if (!isUsefulResultUrl(href)) continue
            if (title.length < 3) continue

            result += SearchHit(
                title = title,
                url = href,
                snippet = "",
                engine = engine
            )

            if (result.size >= 12) break
        }

        /*
         * Fallback pentru motoarele care nu expun corect
         * lista JS de linkuri.
         */
        if (result.isEmpty()) {
            result += parseHtmlLinks(
                raw,
                engine
            )
        }

        return result
    }

    private fun parseHtmlLinks(
        raw: String,
        engine: String
    ): List<SearchHit> {

        val html = jsonField(
            raw,
            "html"
        )

        val result = mutableListOf<SearchHit>()

        val pattern = Regex(
            """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )

        for (match in pattern.findAll(html)) {

            var href = decodeHtml(
                match.groupValues[1]
            )

            val title = cleanText(
                match.groupValues[2]
            )

            href = resolveSearchUrl(
                href,
                engine
            )

            if (!isUsefulResultUrl(href)) continue
            if (title.length < 3) continue

            result += SearchHit(
                title = title,
                url = href,
                snippet = "",
                engine = engine
            )

            if (result.size >= 12) break
        }

        return result
    }

    private fun resolveSearchUrl(
        original: String,
        engine: String
    ): String {

        var href = original.trim()

        if (href.startsWith("//")) {
            href = "https:$href"
        }

        if (href.startsWith("/")) {
            href = when (engine) {
                "Google" ->
                    "https://www.google.com$href"

                "Bing" ->
                    "https://www.bing.com$href"

                "DuckDuckGo" ->
                    "https://html.duckduckgo.com$href"

                else ->
                    href
            }
        }

        try {
            val uri = Uri.parse(href)

            if (
                engine == "Google" &&
                (
                    href.contains("/url?") ||
                        href.contains("google.com/url?")
                    )
            ) {
                val target =
                    uri.getQueryParameter("q")
                        ?: uri.getQueryParameter("url")
                        ?: uri.getQueryParameter("u")

                if (!target.isNullOrBlank()) {
                    href = Uri.decode(target)
                }
            }

            if (
                engine == "DuckDuckGo" &&
                (
                    href.contains("uddg=") ||
                        href.contains("/l/?")
                )
            ) {
                val target =
                    uri.getQueryParameter("uddg")

                if (!target.isNullOrBlank()) {
                    href = Uri.decode(target)
                }
            }

            if (
                engine == "Bing" &&
                href.contains("bing.com/ck/a")
            ) {
                val target =
                    uri.getQueryParameter("u")

                if (!target.isNullOrBlank()) {
                    href = decodeBingUrl(target)
                }
            }

        } catch (_: Exception) {
        }

        return href
    }

    private fun decodeBingUrl(
        value: String
    ): String {
        return try {
            val decoded = Uri.decode(value)

            if (decoded.startsWith("a1")) {
                val body = decoded.substring(2)
                val bytes = android.util.Base64.decode(
                    body,
                    android.util.Base64.URL_SAFE or
                        android.util.Base64.NO_WRAP
                )

                String(bytes)
            } else {
                decoded
            }
        } catch (_: Exception) {
            Uri.decode(value)
        }
    }

    private fun extractReadableText(
        raw: String
    ): String {

        if (raw.isBlank()) return ""

        return jsonField(
            raw,
            "text"
        )
            .replace('\u00A0', ' ')
            .lines()
            .map(::cleanText)
            .filter {
                it.length >= 25
            }
            .filterNot {
                val value = it.lowercase()

                value in setOf(
                    "accept",
                    "agree",
                    "menu",
                    "sign in",
                    "log in",
                    "privacy",
                    "terms",
                    "cookie settings",
                    "advertisement"
                )
            }
            .distinct()
            .joinToString("\n")
    }

    private fun isNoiseContent(
        text: String,
        title: String
    ): Boolean {

        val combined = (
            title + " " +
                text.take(5000)
            ).lowercase()

        val noiseSignals = listOf(
            "before you continue to google",
            "prima di continuare su google",
            "about duckduckgo",
            "informazioni su duckduckgo",
            "microsoft e i suoi fornitori",
            "cookie policy",
            "cookie settings",
            "privacy policy",
            "terms of service",
            "accept all cookies",
            "rifiuta tutto",
            "accetta tutto"
        )

        val matches = noiseSignals.count {
            combined.contains(it)
        }

        return matches >= 2
    }

    private fun buildAnswer(
        query: String,
        evidence: List<WebEvidence>
    ): String {

        return buildString {

            append("🌐 WEB FALLBACK LIVE\n\n")

            append("Căutare: ")
                .append(query)
                .append('\n')

            append("Articole/documente analizate: ")
                .append(evidence.size)
                .append("\n\n")

            evidence.forEachIndexed { index, item ->

                append("[")
                    .append(index + 1)
                    .append("] ")
                    .append(item.title)
                    .append('\n')

                append(item.url)
                    .append('\n')

                append(
                    item.content
                        .ifBlank { item.snippet }
                        .take(1800)
                )
                    .append("\n\n")
            }

            append(
                "⚠️ Sunt considerate surse doar paginile externe " +
                    "cu conținut public suficient. Paginile " +
                    "Google/Bing/DuckDuckGo, cookie, privacy, " +
                    "terms și login sunt excluse."
            )
        }
    }

    private fun extractJsonArray(
        raw: String,
        field: String
    ): String {

        val marker = "\"$field\":["

        val start = raw.indexOf(marker)

        if (start < 0) return ""

        val from = start + marker.length

        var depth = 1
        var escaped = false

        for (i in from until raw.length) {

            val c = raw[i]

            if (escaped) {
                escaped = false
                continue
            }

            if (c == '\\') {
                escaped = true
                continue
            }

            if (c == '[') depth++

            if (c == ']') {
                depth--

                if (depth == 0) {
                    return raw.substring(
                        from,
                        i
                    )
                }
            }
        }

        return ""
    }

    private fun jsonField(
        raw: String,
        field: String
    ): String {

        val key = "\"$field\":"
        val start = raw.indexOf(key)

        if (start < 0) return ""

        var i = start + key.length

        while (
            i < raw.length &&
            raw[i].isWhitespace()
        ) {
            i++
        }

        if (
            i >= raw.length ||
            raw[i] != '"'
        ) {
            return ""
        }

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
                        'u' -> {
                            if (i + 4 <= raw.length) {
                                val hex =
                                    raw.substring(
                                        i,
                                        i + 4
                                    )

                                i += 4

                                try {
                                    hex.toInt(16).toChar()
                                } catch (_: Exception) {
                                    'u'
                                }
                            } else {
                                'u'
                            }
                        }

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

    private fun decodeJavascriptString(
        raw: String
    ): String {

        if (raw == "null") return ""

        return jsonField(
            """{"value":$raw}""",
            "value"
        )
    }

    private fun decodeEscaped(
        value: String
    ): String =
        value
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u002f", "/")

    private fun cleanText(
        value: String
    ): String =
        decodeHtml(value)
            .replace(
                Regex("""<[^>]+>"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

    private fun decodeHtml(
        value: String
    ): String =
        value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")

    private fun normalizeUrl(
        url: String
    ): String =
        url.substringBefore("#")
            .trimEnd('/')
            .lowercase()

    private fun isUsefulResultUrl(
        url: String
    ): Boolean {

        if (
            !url.startsWith("http://") &&
            !url.startsWith("https://")
        ) {
            return false
        }

        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host
            ?.lowercase()
            .orEmpty()

        if (host.isBlank()) return false

        val searchHosts = setOf(
            "google.com",
            "www.google.com",
            "bing.com",
            "www.bing.com",
            "duckduckgo.com",
            "www.duckduckgo.com",
            "html.duckduckgo.com"
        )

        if (
            host in searchHosts ||
            host.endsWith(".google.com") ||
            host.endsWith(".bing.com")
        ) {
            return false
        }

        val path = uri.path
            ?.lowercase()
            .orEmpty()
            .trimEnd('/')

        if (path.isBlank()) return false

        val blocked = listOf(
            "/search",
            "/privacy",
            "/privacy-policy",
            "/terms",
            "/cookie",
            "/cookies",
            "/login",
            "/signin",
            "/sign-in",
            "/account"
        )

        if (
            blocked.any {
                path == it ||
                    path.startsWith("$it/")
            }
        ) {
            return false
        }

        return true
    }
}
