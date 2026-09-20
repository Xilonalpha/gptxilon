from pathlib import Path

ROOT = Path("AIAssistantApp/app/src/main")
PKG = ROOT / "java/com/example/aiassistant"
LAYOUT = ROOT / "res/layout/activity_main.xml"

FILES = {
"ResearchEngine.kt": r'''package com.example.aiassistant

import java.net.URI
import java.util.LinkedHashMap
import java.util.Locale

class ResearchEngine(private val web: WebFallbackEngine) {
    data class Report(val answer: String, val evidence: List<WebEvidence>, val queries: List<String>, val engines: List<String>)

    fun research(query: String, deep: Boolean = true): Report {
        val q = query.trim()
        if (q.isBlank()) return Report("Întrebarea este goală.", emptyList(), emptyList(), emptyList())
        val queries = buildList {
            add(q)
            if (deep) {
                add(q + " surse oficiale")
                add(q + " analiză independentă")
            }
        }.distinct()
        val all = mutableListOf<WebEvidence>()
        val engines = linkedSetOf<String>()
        for (candidate in queries) {
            val result = try { web.ask(candidate) } catch (_: Exception) { null } ?: continue
            engines += result.enginesTried
            all += result.evidence
        }
        val unique = LinkedHashMap<String, WebEvidence>()
        all.forEach { item ->
            val normalized = normalizeUrl(item.url)
            if (normalized.isNotBlank() && !unique.containsKey(normalized)) unique[normalized] = item
        }
        val evidence = unique.values.sortedWith(
            compareByDescending<WebEvidence> { it.content.length }.thenBy { it.title.lowercase(Locale.ROOT) }
        ).take(12)
        return Report(buildAnswer(q, evidence, queries, engines.toList()), evidence, queries, engines.toList())
    }

    private fun buildAnswer(query: String, evidence: List<WebEvidence>, queries: List<String>, engines: List<String>): String = buildString {
        append("🔬 RESEARCH MODE

")
        append("Întrebare: ").append(query).append('
')
        append("Treceri de cercetare: ").append(queries.size).append('
')
        append("Motoare încercate: ").append(if (engines.isEmpty()) "—" else engines.joinToString(", ")).append('
')
        append("Surse unice analizate: ").append(evidence.size).append("

")
        if (evidence.isEmpty()) {
            append("Nu am obținut surse publice utilizabile. Nu voi inventa un răspuns.
")
            return@buildString
        }
        evidence.take(8).forEachIndexed { index, item ->
            append("[").append(index + 1).append("] ").append(item.title).append('
')
            append(item.url).append('
')
            append(item.content.ifBlank { item.snippet }.take(1200)).append("

")
        }
        append("📌 Metodă: sursele au fost deduplicate după URL și păstrate separat. ")
        append("Conținutul poate fi incomplet dacă site-ul blochează WebView, cere autentificare sau livrează o pagină dinamică.")
    }

    private fun normalizeUrl(url: String): String = try {
        val u = URI(url)
        (u.host.orEmpty().lowercase(Locale.ROOT) + u.path.orEmpty().trimEnd('/')).trim()
    } catch (_: Exception) {
        url.substringBefore("#").trimEnd('/').lowercase(Locale.ROOT)
    }
}
''',
"FactCheckEngine.kt": r'''package com.example.aiassistant

import java.util.Locale

object FactCheckEngine {
    data class Check(val supported: Int, val weak: Int, val conflict: Int, val unknown: Int, val report: String)

    fun check(answer: String, evidence: List<WebEvidence>): Check {
        val claims = splitClaims(answer)
        if (claims.isEmpty()) return Check(0, 0, 0, 0, "Nu au fost detectate afirmații verificabile.")
        var supported = 0
        var weak = 0
        var conflict = 0
        var unknown = 0
        val details = StringBuilder()
        claims.take(12).forEachIndexed { index, claim ->
            val terms = keywords(claim)
            val matches = evidence.count { source ->
                val text = (source.title + " " + source.snippet + " " + source.content).lowercase(Locale.ROOT)
                terms.count { text.contains(it) } >= terms.size.coerceAtLeast(2) / 2
            }
            val conflictSignal = Regex(
                "(?i)\b(contradict|disput|deny|denied|false|incorrect|nu este|nu a fost|nu există|however|but)\b"
            ).containsMatchIn(claim)
            val label = when {
                conflictSignal && matches > 0 -> { conflict++; "⚠️ CONFLICT/CONTEXT" }
                matches >= 2 -> { supported++; "✅ SUSȚINUT DE MAI MULTE SURSE" }
                matches == 1 -> { weak++; "🟡 SUSȚINERE LIMITATĂ" }
                else -> { unknown++; "❓ NEVERIFICAT" }
            }
            details.append(index + 1).append(". ").append(label).append(": ").append(claim.take(260)).append('
')
        }
        val report = buildString {
            append("🔎 FACT CHECK

")
            append("Afirmații analizate: ").append(claims.size).append('
')
            append("✅ Susținute de mai multe surse: ").append(supported).append('
')
            append("🟡 Susținere limitată: ").append(weak).append('
')
            append("⚠️ Conflict/context: ").append(conflict).append('
')
            append("❓ Neverificate: ").append(unknown).append("

")
            append(details)
            append("
Acesta este un filtru de evidență local; nu transformă automat potrivirea de cuvinte în dovadă definitivă.")
        }
        return Check(supported, weak, conflict, unknown, report)
    }

    private fun splitClaims(text: String): List<String> =
        text.replace(Regex("(?m)^\s*[-•]\s*"), "")
            .split(Regex("(?<=[.!?])\s+|\n+"))
            .map { it.trim() }
            .filter { it.length >= 45 }
            .filterNot { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .take(20)

    private fun keywords(text: String): List<String> =
        Regex("[\p{L}\p{Nd}]{4,}")
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filterNot { it in STOP }
            .distinct()
            .take(10)
            .toList()

    private val STOP = setOf("care","este","sunt","pentru","despre","acest","această","with","from","that","this","there","their","have","will","unei","unui","mai","foarte","poate","fost")
}
''',
"MemoryEngine.kt": r'''package com.example.aiassistant

class MemoryEngine(private val brain: Brain, private val intelligence: IntelligenceEngine) {
    fun observe(userText: String, aiText: String) {
        val normalized = userText.trim()
        if (normalized.isBlank()) return
        Regex("(?i)\b(?:proiectul meu este|lucrez la|proiectul)\s+([^.!?]{3,100})").find(normalized)?.let {
            intelligence.remember("proiect", it.groupValues[1].trim(), "conversation", 70)
        }
        Regex("(?i)\b(?:prefer|prefer să|vreau)\s+([^.!?]{3,100})").find(normalized)?.let {
            intelligence.remember("preferință", it.groupValues[1].trim(), "conversation", 65)
        }
        if (normalized.length >= 30) intelligence.remember("ultimul subiect", normalized.take(500), "conversation", 45)
        brain.observe(normalized, aiText)
    }
    fun context(query: String): String = intelligence.graphText(query).take(7000)
    fun dashboard(): String = "🧠 Structured Memory
• Brain facts + corrections
• Project/preferences memory
• Temporal updates
• Confidence + source metadata"
}
''',
"CodeIntelligenceEngine.kt": r'''package com.example.aiassistant

object CodeIntelligenceEngine {
    fun audit(text: String): String {
        if (text.isBlank()) return "⌨️ CODE AUDIT

Nu există cod/text pentru audit."
        val lines = text.lines()
        val longLines = lines.count { it.length > 140 }
        val todos = Regex("(?i)\b(TODO|FIXME|XXX)\b").findAll(text).count()
        val emptyCatches = Regex("catch\s*\([^)]*\)\s*\{\s*\}", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(text).count()
        val hardcodedSecrets = Regex("(?i)(api[_-]?key|secret|password|token)\s*[=:]\s*["'][^"']{8,}["']").findAll(text).count()
        val dangerousExec = Regex("(?i)Runtime\.getRuntime\(\)\.exec|ProcessBuilder\(").findAll(text).count()
        val networkCalls = Regex("(?i)https?://|OkHttpClient|HttpURLConnection|WebView").findAll(text).count()
        val functions = Regex("(?m)^\s*(?:public\s+|private\s+|internal\s+|protected\s+)?(?:suspend\s+)?fun\s+").findAll(text).count()
        val issues = mutableListOf<String>()
        if (hardcodedSecrets > 0) issues += "🔴 Posibile secrete hardcodate: $hardcodedSecrets"
        if (emptyCatches > 0) issues += "🟠 catch-uri goale: $emptyCatches"
        if (dangerousExec > 0) issues += "🟠 execuție de proces detectată: $dangerousExec — verifică inputul."
        if (todos > 0) issues += "🟡 TODO/FIXME/XXX: $todos"
        if (longLines > 0) issues += "🟡 linii >140 caractere: $longLines"
        return buildString {
            append("⌨️ CODE INTELLIGENCE AUDIT

")
            append("Linii: ").append(lines.size).append('
')
            append("Funcții detectate: ").append(functions).append('
')
            append("Operații de rețea: ").append(networkCalls).append('
')
            append("Probleme semnalate: ").append(issues.size).append("

")
            if (issues.isEmpty()) append("✅ Nu au fost găsite semnale locale evidente.
") else issues.forEach { append(it).append('
') }
            append("
⚠️ Audit static: rezultatul indică semnale de verificat, nu dovedește existența sau absența unui bug.")
        }
    }
}
'''
}

for name, content in FILES.items():
    (PKG / name).parent.mkdir(parents=True, exist_ok=True)
    (PKG / name).write_text(content, encoding="utf-8")

main = PKG / "MainActivity.kt"
s = main.read_text(encoding="utf-8")
s = s.replace(
'    private lateinit var webFallback: WebFallbackEngine\n',
'''    private lateinit var webFallback: WebFallbackEngine
    private lateinit var researchEngine: ResearchEngine
    private lateinit var memoryEngine: MemoryEngine
    private var lastWebEvidence: List<WebEvidence> = emptyList()
''', 1)
s = s.replace(
'webFallback=WebFallbackEngine(this)',
'webFallback=WebFallbackEngine(this); researchEngine=ResearchEngine(webFallback); memoryEngine=MemoryEngine(brain,intelligence)',
1)

old_actions = '''        binding.btnChipSummarize.setOnClickListener { val x=adapter.getMessages().filter{!it.isUser}.joinToString("\\n"){it.text.take(600)};if(x.isNotBlank()){binding.etMessage.setText("Analizează și rezumă aceste informații, marcând FAPT/INFERENȚĂ/NECUNOSCUT:\\n$x");sendMessage()}else toast("Nimic de rezumat") }
'''
new_actions = old_actions + '''        binding.btnResearch.setOnClickListener { binding.etMessage.setText("Cercetează în profunzime această întrebare, caută surse independente și oficiale, compară informațiile și marchează incertitudinile."); binding.etMessage.requestFocus() }
        binding.btnVerify.setOnClickListener {
            val last=adapter.getMessages().lastOrNull{!it.isUser}
            if(last==null){toast("Nu există încă un răspuns de verificat")} else if(lastWebEvidence.isNotEmpty()){
                val check=FactCheckEngine.check(last.text,lastWebEvidence)
                AlertDialog.Builder(this).setTitle("🔎 Fact Check").setMessage(check.report).setPositiveButton("OK",null).show()
            } else {
                binding.etMessage.setText("Verifică factual acest răspuns și caută surse publice independente:\\n${last.text.take(1800)}")
                sendMessage()
            }
        }
        binding.btnAudit.setOnClickListener {
            val attachment=pendingAttachment
            if(attachment?.text != null){
                AlertDialog.Builder(this).setTitle("⌨️ Code Intelligence").setMessage(CodeIntelligenceEngine.audit(attachment.text)).setPositiveButton("OK",null).show()
            } else {
                val last=adapter.getMessages().lastOrNull{it.isUser}
                if(last!=null && (last.text.contains("fun ") || last.text.contains("class ") || last.text.contains("{"))){
                    AlertDialog.Builder(this).setTitle("⌨️ Code Intelligence").setMessage(CodeIntelligenceEngine.audit(last.text)).setPositiveButton("OK",null).show()
                } else toast("Atașează cod/text sau pune codul în mesaj.")
            }
        }
        binding.btnMemory.setOnClickListener { AlertDialog.Builder(this).setTitle("🧠 Structured Memory").setMessage(memoryEngine.dashboard()+"\\n\\n"+memoryEngine.context("")).setPositiveButton("OK",null).show() }
'''
if old_actions not in s:
    raise SystemExit("Nu am găsit blocul quick actions din MainActivity.kt")
s=s.replace(old_actions,new_actions,1)

old_send = '''                val reply=if(apiKey.isNotBlank()) {
                    GeminiClient(apiKey).askWithAttachmentOrNormal(history,attachment,brain,personaIndex,thinkingMode,agentMode,powerMode,intelligence)
                } else {
                    val web=webFallback.ask(text)
                    AiResponse(web.answer,web.evidence.map{"${it.title} — ${it.url}"},0)
                }
                brain.observe(text,reply.text,reply.tokens);lastCodeBlocks=FileSaver.extractCodeBlocks(reply.text);intelligence.remember("last_query",text);val evidence=intelligence.evidence(reply.sources,reply.text)
                val decorated=reply.text+"\\n\\n🔎 Evidence: ${evidence.label} ${evidence.confidence}%\\n${evidence.explanation}"
'''
new_send = '''                val collectedEvidence=mutableListOf<WebEvidence>()
                val plan=SupremeEngine.route(text,attachment,agentMode,powerMode)
                val reply=if(apiKey.isNotBlank()) {
                    GeminiClient(apiKey).askWithAttachmentOrNormal(history,attachment,brain,personaIndex,thinkingMode,agentMode,powerMode,intelligence)
                } else {
                    val report=researchEngine.research(text,plan.deep)
                    collectedEvidence.addAll(report.evidence)
                    AiResponse(report.answer,report.evidence.map{"${it.title} — ${it.url}"},0)
                }
                lastWebEvidence=collectedEvidence.toList()
                memoryEngine.observe(text,reply.text);lastCodeBlocks=FileSaver.extractCodeBlocks(reply.text);intelligence.remember("last_query",text)
                val evidence=intelligence.evidence(reply.sources,reply.text)
                val fact=if(collectedEvidence.isNotEmpty()) FactCheckEngine.check(reply.text,collectedEvidence) else null
                val factLine=fact?.let{"\\n\\n🔎 Fact Check: ${it.supported} multi-source • ${it.weak} limited • ${it.conflict} conflict • ${it.unknown} unknown"} ?: ""
                val decorated=reply.text+"\\n\\n🔎 Evidence: ${evidence.label} ${evidence.confidence}%\\n${evidence.explanation}"+factLine
'''
if old_send not in s:
    raise SystemExit("Nu am găsit blocul sendMessage din MainActivity.kt")
s=s.replace(old_send,new_send,1)
main.write_text(s,encoding="utf-8")

layout = LAYOUT.read_text(encoding="utf-8")
marker='''            <Button
                android:id="@+id/btnChipSummarize"'''
insert='''            <Button
                android:id="@+id/btnResearch"
                android:layout_width="120dp"
                android:layout_height="40dp"
                android:minWidth="0dp"
                android:minHeight="0dp"
                android:layout_marginEnd="5dp"
                android:paddingStart="8dp"
                android:paddingEnd="8dp"
                android:text="🔬 Research"
                android:textSize="13sp" />

            <Button
                android:id="@+id/btnVerify"
                android:layout_width="115dp"
                android:layout_height="40dp"
                android:minWidth="0dp"
                android:minHeight="0dp"
                android:layout_marginEnd="5dp"
                android:paddingStart="8dp"
                android:paddingEnd="8dp"
                android:text="🔎 Fact Check"
                android:textSize="13sp" />

            <Button
                android:id="@+id/btnAudit"
                android:layout_width="110dp"
                android:layout_height="40dp"
                android:minWidth="0dp"
                android:minHeight="0dp"
                android:layout_marginEnd="5dp"
                android:paddingStart="8dp"
                android:paddingEnd="8dp"
                android:text="⌨️ Audit"
                android:textSize="13sp" />

            <Button
                android:id="@+id/btnMemory"
                android:layout_width="110dp"
                android:layout_height="40dp"
                android:minWidth="0dp"
                android:minHeight="0dp"
                android:layout_marginEnd="5dp"
                android:paddingStart="8dp"
                android:paddingEnd="8dp"
                android:text="🧠 Memory"
                android:textSize="13sp" />

'''
if marker not in layout:
    raise SystemExit("Nu am găsit btnChipSummarize în layout")
layout=layout.replace(marker,insert+marker,1)
LAYOUT.write_text(layout,encoding="utf-8")

print("Upgrade aplicat local.")
print("Fișiere noi:", ", ".join(FILES.keys()))
print("Modificate: MainActivity.kt, activity_main.xml")
