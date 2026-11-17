package com.neo.notifsync

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var accessGranted by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        accessGranted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val dao = AppDb.get(this).dao()

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var server by remember { mutableStateOf(prefs.server) }
                    var topic by remember { mutableStateOf(prefs.topic) }
                    var token by remember { mutableStateOf(prefs.token) }
                    var forward by remember { mutableStateOf(prefs.forward) }
                    val items by dao.recent().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()

                    LazyColumn(
                        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Text("NotifSync", style = MaterialTheme.typography.headlineSmall) }
                        item {
                            if (!accessGranted) {
                                Button(onClick = {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }) { Text("Grant notification access") }
                            } else Text("Notification access: ON ✓")
                        }
                        item {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Switch(forward, { forward = it; prefs.forward = it })
                                Spacer(Modifier.width(8.dp)); Text("Forward to other devices")
                            }
                        }
                        item {
                            OutlinedTextField(server, { server = it; prefs.server = it },
                                label = { Text("ntfy server") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        item {
                            OutlinedTextField(topic, { topic = it; prefs.topic = it },
                                label = { Text("Topic (secret!)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        item {
                            OutlinedTextField(token, { token = it; prefs.token = it },
                                label = { Text("Access token (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { topic = Prefs.randomTopic(); prefs.topic = topic }) { Text("Random topic") }
                                OutlinedButton(onClick = { scope.launch { dao.clear() } }) { Text("Clear archive") }
                            }
                        }
                        item { HorizontalDivider() }
                        item { Text("Archive (${items.size})", style = MaterialTheme.typography.titleMedium) }
                        items(items, key = { it.id }) { n ->
                            Column {
                                Text("${n.app} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(n.time))}" +
                                        if (n.sent) " ✓" else "", style = MaterialTheme.typography.labelSmall)
                                if (n.title.isNotBlank()) Text(n.title, style = MaterialTheme.typography.bodyMedium)
                                if (n.text.isNotBlank()) Text(n.text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
