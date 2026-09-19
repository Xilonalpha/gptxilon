package com.example.aiassistant

import android.content.Context
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WatchManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("watchers", Context.MODE_PRIVATE)
    data class Watch(val id: String, val query: String, val intervalMinutes: Long, val enabled: Boolean)

    fun list(): List<Watch> {
        val a = JSONArray(prefs.getString("items", "[]")); val out = mutableListOf<Watch>()
        for (i in 0 until a.length()) { val j=a.getJSONObject(i); out += Watch(j.getString("id"),j.getString("q"),j.optLong("m",60),j.optBoolean("e",true)) }
        return out
    }
    fun add(query: String, minutes: Long = 60) {
        val arr = JSONArray(); list().forEach { arr.put(JSONObject().put("id",it.id).put("q",it.query).put("m",it.intervalMinutes).put("e",it.enabled)) }
        val id="watch_${System.currentTimeMillis()}"; arr.put(JSONObject().put("id",id).put("q",query).put("m",minutes.coerceAtLeast(15)).put("e",true)); prefs.edit().putString("items",arr.toString()).apply(); schedule(id, query, minutes.coerceAtLeast(15))
    }
    fun remove(watch: Watch) {
        val arr=JSONArray(); list().filterNot { it.id==watch.id }.forEach { arr.put(JSONObject().put("id",it.id).put("q",it.query).put("m",it.intervalMinutes).put("e",it.enabled)) }; prefs.edit().putString("items",arr.toString()).apply()
        WorkManager.getInstance(context).cancelUniqueWork(watch.id)
    }
    private fun schedule(id:String, query:String, minutes:Long) {
        val req=PeriodicWorkRequestBuilder<WatchWorker>(minutes.coerceAtLeast(15),TimeUnit.MINUTES)
            .setInputData(workDataOf("id" to id,"query" to query)).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(id, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class WatchWorker(appContext: Context, params: WorkerParameters): Worker(appContext, params) {
    override fun doWork(): Result {
        val key=SecureStore(applicationContext).get(); val query=inputData.getString("query") ?: return Result.failure()
        if (key.isBlank()) return Result.success()
        return try {
            val r=GeminiClient(key).ask(listOf(ChatMessage("Verifică ce s-a schimbat recent despre: $query",true)),forceSearch=true)
            val prefs=applicationContext.getSharedPreferences("watch_results",Context.MODE_PRIVATE); val old=prefs.getString(query,"") ?: ""
            val normalized=r.text.take(1500)
            if (old.isNotBlank() && old != normalized) NotificationHelper.show(applicationContext,"AI Watcher","Schimbare detectată pentru: $query\n${normalized.take(180)}")
            prefs.edit().putString(query,normalized).apply(); Result.success()
        } catch (_:Exception) { Result.retry() }
    }
}

object NotificationHelper {
    private const val CHANNEL="ai_watcher"
    fun show(context: Context,title:String,text:String) {
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(android.app.NotificationChannel(CHANNEL,"AI Watcher",android.app.NotificationManager.IMPORTANCE_DEFAULT))
        val builder=if(android.os.Build.VERSION.SDK_INT>=26) android.app.Notification.Builder(context,CHANNEL) else android.app.Notification.Builder(context)
        builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setStyle(android.app.Notification.BigTextStyle().bigText(text)).setAutoCancel(true)
        manager.notify((System.currentTimeMillis()%100000).toInt(),builder.build())
    }
}
