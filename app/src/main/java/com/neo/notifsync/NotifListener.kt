package com.neo.notifsync

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.*
import kotlinx.coroutines.*

class NotifListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seen = HashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return // media players, charging, etc.

        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // Apps re-post the same notification when updating it; drop repeats within 3 s.
        val key = "${sbn.packageName}|$title|$text"
        val now = System.currentTimeMillis()
        synchronized(seen) {
            if (now - (seen[key] ?: 0L) < 3000) return
            seen[key] = now
            if (seen.size > 200) seen.entries.removeAll { now - it.value > 60_000 }
        }

        val app = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) { sbn.packageName }

        scope.launch {
            val id = AppDb.get(this@NotifListener).dao()
                .insert(Notif(pkg = sbn.packageName, app = app, title = title, text = text, time = sbn.postTime))
            val prefs = Prefs(this@NotifListener)
            if (prefs.forward && prefs.topic.isNotBlank()) {
                val req = OneTimeWorkRequestBuilder<SendWorker>()
                    .setInputData(workDataOf("id" to id))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(this@NotifListener).enqueue(req)
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
