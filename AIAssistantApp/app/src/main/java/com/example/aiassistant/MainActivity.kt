package com.example.aiassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aiassistant.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var brain: Brain
    private lateinit var sessions: SessionManager
    private lateinit var intelligence: IntelligenceEngine
    private lateinit var workspaces: WorkspaceManager
    private lateinit var watchers: WatchManager
    private lateinit var secureStore: SecureStore
    private lateinit var webFallback: WebFallbackEngine
    private lateinit var researchEngine: ResearchEngine
    private lateinit var memoryEngine: MemoryEngine
    private lateinit var geminiLearning: GeminiLearningEngine
    private var lastWebEvidence: List<WebEvidence> = emptyList()
    private var apiKey = ""
    private var personaIndex = 0
    private var thinkingMode = false
    private var agentMode = false
    private var powerMode = false
    private var currentWorkspace: WorkspaceManager.Workspace? = null
    private var pendingAttachment: Attachment? = null
    private var lastCodeBlocks: List<Pair<String, String>> = emptyList()
    private var tts: TextToSpeech? = null
    private var sessionSaved = false

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) r.data?.data?.let { uri -> contentResolver.openOutputStream(uri)?.use { it.write(buildTranscript().toByteArray()) }; toast("Conversație exportată ✅") }
    }
    private val codeSaveLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) r.data?.data?.let { uri -> contentResolver.openOutputStream(uri)?.use { it.write(lastCodeBlocks.joinToString("\n\n").toByteArray()) }; toast("Cod salvat ✅") }
    }
    private val treeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) { }
            thread { val ok=FileSaver.exportProjectZip(this,uri,lastCodeBlocks); runOnUiThread { toast(if(ok) "📦 Proiect exportat" else "Nu există cod exportabil") } }
        }
    }
    private val attachmentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) thread { val a=FileIntelligence.read(this,uri); runOnUiThread { pendingAttachment=a; binding.tvAttachment.text=if(a!=null) "📎 ${a.name}" else "Atașamentul nu a putut fi citit"; toast(if(a!=null) "Fișier pregătit pentru AI" else "Fișier invalid") } }
    }
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { binding.etMessage.setText(it); binding.etMessage.setSelection(it.length) }
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        brain=Brain(this); sessions=SessionManager(this); intelligence=IntelligenceEngine(filesDir); workspaces=WorkspaceManager(this); watchers=WatchManager(this); secureStore=SecureStore(this); webFallback=WebFallbackEngine(this); researchEngine=ResearchEngine(webFallback); geminiLearning=GeminiLearningEngine(filesDir); memoryEngine=MemoryEngine(brain,intelligence,geminiLearning)
        adapter=ChatAdapter(); binding.recyclerView.layoutManager=LinearLayoutManager(this); binding.recyclerView.adapter=adapter
        apiKey=secureStore.get()
        if (apiKey.isBlank()) getSharedPreferences("ai_prefs",MODE_PRIVATE).getString("api_key","")?.takeIf { it.isNotBlank() }?.let { apiKey=it; secureStore.put(it); getSharedPreferences("ai_prefs",MODE_PRIVATE).edit().remove("api_key").apply() }
        personaIndex=getSharedPreferences("ai_prefs",MODE_PRIVATE).getInt("persona",0)
        binding.etApiKey.setText(if(apiKey.isBlank()) "" else "••••••••••••••••")
        if(Build.VERSION.SDK_INT>=33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        tts=TextToSpeech(this){ if(it==TextToSpeech.SUCCESS) tts?.language=Locale.getDefault() }
        adapter.addMessage(ChatMessage("Salut! Sunt AI Assistant Pro — Intelligence Edition.\n\n🧠 Memory + Knowledge Graph\n🔎 Verification + confidence\n🤖 Agent Mode\n📁 File/Vision Intelligence\n🛰️ AI Watcher\n🧩 Workspaces + Project Builder\n🚀 Supreme Power Engine\n\nApasă 📎 pentru fișiere, ⚡ pentru Agent sau 🧠 pentru Intelligence Core.",false))
        wireActions()
    }

    private fun wireActions() {
        adapter.onFeedback={position,positive-> val msgs=adapter.getMessages(); val u=msgs.take(position).lastOrNull{it.isUser}?.text ?: ""; brain.learnFromFeedback(u,msgs[position].text,positive); adapter.markRated(position); toast("🧠 Feedback învățat") }
        adapter.onSpeak={text-> tts?.stop(); tts?.speak(text.take(3500),TextToSpeech.QUEUE_FLUSH,null,"ai_reply") }
        binding.btnSaveKey.setOnClickListener {
            val typed=binding.etApiKey.text.toString().trim()

            if (apiKey.isNotBlank() && typed.startsWith("••")) {
                secureStore.clear()
                apiKey=""
                binding.etApiKey.setText("")
                toast("🗑️ Cheia Gemini a fost ștearsă. Motorul local este activ.")
            } else if (typed.isNotBlank() && !typed.startsWith("••")) {
                apiKey=typed
                secureStore.put(typed)
                binding.etApiKey.setText("••••••••••••••••")
                toast("🔐 Cheia este stocată criptat")
            }
        }
        binding.btnBrain.setOnClickListener { showIntelligence() }
        binding.btnThink.setOnClickListener { thinkingMode=!thinkingMode; updateModes(); toast(if(thinkingMode) "🧭 Pași rezumați activi" else "Pașii rezumați opriți") }
        binding.btnPower.setOnClickListener { powerMode=!powerMode; if(powerMode) agentMode=true; updateModes(); toast(if(powerMode) "🚀 SUPREME POWER ACTIV — analiză profundă + verificare" else "Power Mode oprit") }
        binding.btnAgent.setOnClickListener { agentMode=!agentMode; if(!agentMode) powerMode=false; updateModes(); toast(if(agentMode) "🤖 Agent Mode ACTIV" else "Agent Mode oprit") }
        binding.btnPersona.setOnClickListener { choosePersona() }
        binding.btnHistory.setOnClickListener { showHistoryDialog() }
        binding.btnWorkspace.setOnClickListener { showWorkspaceDialog() }
        binding.btnExport.setOnClickListener { exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="text/markdown";putExtra(Intent.EXTRA_TITLE,"conversatie_ai.md")}) }
        binding.btnZip.setOnClickListener { treeLauncher.launch(null) }
        binding.btnAttach.setOnClickListener { attachmentLauncher.launch(arrayOf("*/*")) }
        binding.btnWatcher.setOnClickListener { showWatcherDialog() }
        binding.btnMic.setOnClickListener { try { speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag())}) } catch(_:Exception){toast("Recunoașterea vocală nu este disponibilă")} }
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnChipNew.setOnClickListener { saveCurrentSession();adapter.setMessages(emptyList());lastCodeBlocks=emptyList();pendingAttachment=null;binding.tvAttachment.text="";toast("✚ Workspace conversațional nou") }
        binding.btnChipNews.setOnClickListener { binding.etMessage.setText("Caută cele mai importante știri de astăzi, verifică sursele și semnalează conflictele.");sendMessage() }
        binding.btnChipExplain.setOnClickListener { val x=adapter.getMessages().lastOrNull{!it.isUser&&it.text.length>80}; if(x!=null){binding.etMessage.setText("Explică simplu și verifică afirmațiile: ${x.text.take(700)}");sendMessage()}else toast("Trimite mai întâi o întrebare") }
        binding.btnChipCode.setOnClickListener { binding.etMessage.setText("Construiește un proiect complet și returnează fiecare fișier cu markerul [file:path]");binding.etMessage.requestFocus() }
        binding.btnChipSummarize.setOnClickListener { val x=adapter.getMessages().filter{!it.isUser}.joinToString("\n"){it.text.take(600)};if(x.isNotBlank()){binding.etMessage.setText("Analizează și rezumă aceste informații, marcând FAPT/INFERENȚĂ/NECUNOSCUT:\n$x");sendMessage()}else toast("Nimic de rezumat") }
        binding.btnResearch.setOnClickListener { binding.etMessage.setText("Cercetează în profunzime această întrebare, caută surse independente și oficiale, compară informațiile și marchează incertitudinile."); binding.etMessage.requestFocus() }
        binding.btnVerify.setOnClickListener {
            val last=adapter.getMessages().lastOrNull{!it.isUser}
            if(last==null){toast("Nu există încă un răspuns de verificat")} else if(lastWebEvidence.isNotEmpty()){
                val check=FactCheckEngine.check(last.text,lastWebEvidence)
                AlertDialog.Builder(this).setTitle("🔎 Fact Check").setMessage(check.report).setPositiveButton("OK",null).show()
            } else {
                binding.etMessage.setText("Verifică factual acest răspuns și caută surse publice independente:\n${last.text.take(1800)}")
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
        binding.btnMemory.setOnClickListener { AlertDialog.Builder(this).setTitle("🧠 Structured Memory").setMessage(memoryEngine.dashboard()+"\n\n"+memoryEngine.context("")).setPositiveButton("OK",null).show() }
    }

    private fun sendMessage(){
        val text=binding.etMessage.text.toString().trim(); if(text.isBlank())return
        binding.etMessage.setText("");sessionSaved=false;adapter.addMessage(ChatMessage(text,true));currentWorkspace?.let{workspaces.appendMessage(it,ChatMessage(text,true))};adapter.addMessage(ChatMessage("⏳ Analizez…",false));binding.recyclerView.scrollToPosition(adapter.itemCount-1)
        val history=adapter.getMessages().dropLast(1);val attachment=pendingAttachment;pendingAttachment=null;binding.tvAttachment.text=""
        thread {
            try {
                val collectedEvidence=mutableListOf<WebEvidence>()
                val plan=SupremeEngine.route(text,attachment,agentMode,powerMode)
                val reply=if(apiKey.isNotBlank()) {
                    try {
                        GeminiClient(apiKey).askWithAttachmentOrNormal(
                            history,
                            attachment,
                            brain,
                            personaIndex,
                            thinkingMode,
                            agentMode,
                            powerMode,
                            intelligence
                        )
                    } catch (e: Exception) {
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
                    }
                } else {
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
                }
                lastWebEvidence=collectedEvidence.toList()
                memoryEngine.observe(text,reply.text,if(apiKey.isNotBlank()) reply else null);lastCodeBlocks=FileSaver.extractCodeBlocks(reply.text);intelligence.remember("last_query",text)
                val evidence=intelligence.evidence(reply.sources,reply.text)
                val fact=if(collectedEvidence.isNotEmpty()) FactCheckEngine.check(reply.text,collectedEvidence) else null
                val factLine=fact?.let{"\n\n🔎 Fact Check: ${it.supported} multi-source • ${it.weak} limited • ${it.conflict} conflict • ${it.unknown} unknown"} ?: ""
                val decorated=reply.text+"\n\n🔎 Evidence: ${evidence.label} ${evidence.confidence}%\n${evidence.explanation}"+factLine
                runOnUiThread { adapter.updateLastMessage(decorated,reply.sources,reply.tokens);currentWorkspace?.let{workspaces.appendMessage(it,ChatMessage(decorated,false,reply.sources))};binding.recyclerView.scrollToPosition(adapter.itemCount-1) }
            }catch(e:Exception){runOnUiThread{adapter.updateLastMessage("⚠️ ${e.message ?: "Eroare necunoscută"}");binding.recyclerView.scrollToPosition(adapter.itemCount-1)}}
        }
    }

    private fun GeminiClient.askWithAttachmentOrNormal(history:List<ChatMessage>,a:Attachment?,brain:Brain,p:Int,think:Boolean,agent:Boolean,power:Boolean,intel:IntelligenceEngine):AiResponse{
        return if(a!=null) askWithAttachment(history,a,brain.brainContext(),p,false,intel.graphText()) else {
            val last=history.lastOrNull{it.isUser}?.text.orEmpty()
            val plan=SupremeEngine.route(last,null,agent,power)
            val web=plan.useWeb
            val base=ask(history,brain.brainContext()+" "+SupremeEngine.systemDirective(plan),p,think,web,agent,intel.graphText(last))
            if(!power) base else {
                val critique=ask(listOf(ChatMessage("Evaluează critic acest răspuns. Identifică doar erori, afirmații neverificate, contradicții și lucruri lipsă. Răspunsul este:\n${base.text}",true)),brain.brainContext(),4,false,web,false,intel.graphText(last))
                ask(listOf(ChatMessage("Răspuns inițial:\n${base.text}\n\nCritică independentă:\n${critique.text}\n\nRefă răspunsul final: păstrează doar informația susținută, repară erorile și marchează clar incertitudinea.",true)),brain.brainContext(),p,false,web,true,intel.graphText(last))
            }
        }
    }

    private fun updateModes(){ binding.btnThink.alpha=if(thinkingMode)1f else .45f;binding.btnAgent.alpha=if(agentMode)1f else .45f;binding.btnPower.alpha=if(powerMode)1f else .45f }

    private fun choosePersona(){ val names=GeminiClient.PERSONAS.map{it.first}.toTypedArray();AlertDialog.Builder(this).setTitle("🎭 Persona").setSingleChoiceItems(names,personaIndex){d,w->personaIndex=w;getSharedPreferences("ai_prefs",MODE_PRIVATE).edit().putInt("persona",w).apply();d.dismiss();toast("Persona: ${names[w]}")}.show() }

    private fun showIntelligence(){
        val e=intelligence.dashboard(brain)+"\n\n"+brain.stats()+"\n\n🕸️ KNOWLEDGE GRAPH\n"+intelligence.graphText()+"\n\n🚀 POWER ENGINE\nRouter adaptiv • Deep critique • Evidence synthesis • Multimodal context • Temporal memory"
        AlertDialog.Builder(this).setTitle("⚡ Intelligence Core").setMessage(e).setPositiveButton("OK",null).setNeutralButton("Export raport"){_,_->codeSaveLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="text/plain";putExtra(Intent.EXTRA_TITLE,"ai_intelligence_report.txt")})}.show()
    }

    private fun showWorkspaceDialog(){
        val items=workspaces.list();val names=(listOf("➕ Workspace nou")+items.map{it.name}).toTypedArray()
        AlertDialog.Builder(this).setTitle("🧩 Workspaces").setItems(names){_,which->if(which==0){val input=EditText(this);input.hint="Numele workspace-ului";AlertDialog.Builder(this).setTitle("Workspace nou").setView(input).setPositiveButton("Creează"){_,_->currentWorkspace=workspaces.create(input.text.toString());binding.tvWorkspace.text="🧩 ${currentWorkspace!!.name}";toast("Workspace activ")}.setNegativeButton("Renunță",null).show()}else{currentWorkspace=items[which-1];binding.tvWorkspace.text="🧩 ${currentWorkspace!!.name}";toast("Workspace activ")}}.show()
    }

    private fun showWatcherDialog(){
        val watches=watchers.list();val options=(listOf("➕ Urmărește un subiect")+watches.map{"${if(it.enabled)"🟢" else "⚪"} ${it.query}"}).toTypedArray()
        AlertDialog.Builder(this).setTitle("🛰️ AI Watcher").setItems(options){_,which->if(which==0){val input=EditText(this);input.hint="Ce vrei să urmăresc?";AlertDialog.Builder(this).setTitle("Watcher nou").setView(input).setPositiveButton("Activează"){_,_->watchers.add(input.text.toString(),60);toast("🛰️ Monitorizare activată (minim 15 min)")}.setNegativeButton("Renunță",null).show()}else{val w=watches[which-1];AlertDialog.Builder(this).setTitle(w.query).setMessage("Verificare la fiecare ${w.intervalMinutes} minute.").setPositiveButton("Șterge"){_,_->watchers.remove(w);toast("Watcher șters")}.setNegativeButton("OK",null).show()}}.show()
    }

    private fun showHistoryDialog(){val list=sessions.listSessions();if(list.isEmpty()){toast("Nicio conversație salvată");return};val items=list.mapIndexed{i,s->"${i+1}. ${java.text.SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(java.util.Date(s.time))} — ${s.title.take(45)}"}.toTypedArray();AlertDialog.Builder(this).setTitle("📚 Istoric").setItems(items){_,w->val s=list[w];AlertDialog.Builder(this).setTitle(s.title).setItems(arrayOf("📂 Încarcă","🗑️ Șterge")){_,x->if(x==0){adapter.setMessages(sessions.load(s));binding.recyclerView.scrollToPosition(adapter.itemCount-1)}else{sessions.delete(s);toast("Șters")}}.show()}.show()}
    private fun saveCurrentSession(){if(!sessionSaved){sessions.save(adapter.getMessages());sessionSaved=true}}
    private fun buildTranscript()=buildString{append("# AI Assistant Pro Intelligence Edition\n\n");adapter.getMessages().forEach{append(if(it.isUser)"**Tu:** " else "**AI:** ").append(it.text).append("\n\n");if(it.sources.isNotEmpty())append("Surse: ").append(it.sources.joinToString(", ")).append("\n\n---\n\n")}}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onPause(){super.onPause();saveCurrentSession()}
    override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
}

object RouteEngine {
    fun needsWeb(text:String):Boolean {
        val x=text.lowercase()
        val live=listOf("azi","astăzi","acum","latest","ultimele","știri","pret","preț","vreme","actual","2026","recent","live","cine este","când a fost","verifică","sursă","surse")
        return live.any{x.contains(it)} || x.startsWith("cauta:") || x.startsWith("caută:")
    }
}
