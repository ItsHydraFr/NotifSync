package com.neo.notifsync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Publishes one archived notification to an ntfy topic. Retries automatically when offline. */
class SendWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val dao = AppDb.get(applicationContext).dao()
        val n = dao.get(inputData.getLong("id", -1)) ?: return Result.success()
        if (n.sent) return Result.success()
        val prefs = Prefs(applicationContext)
        if (prefs.topic.isBlank()) return Result.success()

        val body = JSONObject()
            .put("topic", prefs.topic)
            .put("title", if (n.title.isBlank()) n.app else "${n.app}: ${n.title}")
            .put("message", n.text.ifBlank { "(no text)" })
            .toString()

        return try {
            val c = URL(prefs.server).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (prefs.token.isNotBlank()) c.setRequestProperty("Authorization", "Bearer ${prefs.token}")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            c.disconnect()
            when {
                code in 200..299 -> { dao.markSent(n.id); Result.success() }
                code == 429 || code >= 500 -> Result.retry()
                else -> Result.failure() // bad topic/auth: retrying won't help
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
