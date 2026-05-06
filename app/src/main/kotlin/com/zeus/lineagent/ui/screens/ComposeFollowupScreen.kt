package com.zeus.lineagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeus.lineagent.AppContainer
import com.zeus.lineagent.data.AppSettings
import com.zeus.lineagent.network.AsanaTask
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeFollowupScreen(
    container: AppContainer,
    mappingId: String,
    onBack: () -> Unit,
) {
    val mappings by container.settings.mappings.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val mapping = mappings.firstOrNull { it.id == mappingId }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var loading by remember { mutableStateOf(false) }
    var tasks by remember { mutableStateOf<List<AsanaTask>>(emptyList()) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var customMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        if (mapping == null) return
        loading = true
        error = null
        if (settings.asanaToken.isBlank() || mapping.asanaProjectGid.isBlank()) {
            tasks = emptyList()
            loading = false
            return
        }
        container.asana.openTasks(settings.asanaToken, mapping.asanaProjectGid)
            .onSuccess { tasks = it }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(mappingId, settings.asanaToken) { reload() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(mapping?.label ?: "Follow-up") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { reload() } }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        if (mapping == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Mapping not found.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(mapping.label, fontWeight = FontWeight.Bold)
                    Text("LINE target: ${mapping.lineTargetId}")
                    Text("Asana project: ${mapping.asanaProjectGid.ifBlank { "—" }}")
                }
            }

            Text("Open tasks", fontWeight = FontWeight.SemiBold)
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text("Couldn't load: $error")
                tasks.isEmpty() -> Text("No open tasks (or Asana project not set).")
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tasks.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected[task.gid] == true,
                                    onCheckedChange = { selected[task.gid] = it },
                                )
                                Column {
                                    Text(task.name, fontWeight = FontWeight.Medium)
                                    val sub = listOfNotNull(
                                        task.assignee?.name?.let { "@$it" },
                                        task.due_on?.let { "due $it" },
                                    ).joinToString(" · ")
                                    if (sub.isNotBlank()) Text(sub)
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val chosen = tasks.filter { selected[it.gid] == true }
                    customMessage = buildFollowupTemplate(
                        senderName = settings.displayName.ifBlank { "Zeus" },
                        projectLabel = mapping.label,
                        tasks = chosen.ifEmpty { tasks.take(5) },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Generate follow-up template") }

            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                label = { Text("Message to send to LINE") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            Button(
                onClick = {
                    scope.launch {
                        container.line.push(
                            channelToken = settings.lineToken,
                            target = mapping.lineTargetId,
                            text = customMessage,
                        ).onSuccess {
                            snackbar.showSnackbar("Sent to LINE")
                            if (settings.zapierWebhookUrl.isNotBlank()) {
                                container.zapier.fire(
                                    settings.zapierWebhookUrl,
                                    JsonObject(
                                        mapOf(
                                            "event" to JsonPrimitive("line_followup_sent"),
                                            "project" to JsonPrimitive(mapping.label),
                                            "asana_project" to JsonPrimitive(mapping.asanaProjectGid),
                                            "line_target" to JsonPrimitive(mapping.lineTargetId),
                                            "message" to JsonPrimitive(customMessage),
                                            "sender" to JsonPrimitive(settings.displayName),
                                        ),
                                    ),
                                )
                            }
                        }.onFailure {
                            snackbar.showSnackbar(it.message ?: "Send failed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text("  Send to LINE")
            }
        }
    }
}

private fun buildFollowupTemplate(
    senderName: String,
    projectLabel: String,
    tasks: List<AsanaTask>,
): String {
    val header = "[$projectLabel] Follow-up from $senderName"
    if (tasks.isEmpty()) {
        return "$header\n\nQuick check-in — any blockers on this project? Please share an update."
    }
    val lines = tasks.joinToString("\n") { task ->
        val owner = task.assignee?.name?.let { " @$it" }.orEmpty()
        val due = task.due_on?.let { " (due $it)" }.orEmpty()
        "• ${task.name}$owner$due"
    }
    return "$header\n\nCould we get a status update on:\n$lines\n\nThanks!"
}
