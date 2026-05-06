package com.zeus.lineagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.zeus.lineagent.data.GroupMapping
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMappingsScreen(container: AppContainer, onBack: () -> Unit) {
    val mappings by container.settings.mappings.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<GroupMapping?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group → project mappings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (mappings.isEmpty()) {
                Text(
                    "Map each LINE chat (group/room/user) to the Asana project the bot should follow up about. " +
                        "The bot needs to be invited into the LINE group first; the group ID is captured by " +
                        "your LINE webhook.",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mappings, key = { it.id }) { m ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.label, fontWeight = FontWeight.Bold)
                                    Text("LINE: ${m.lineTargetId}")
                                    Text("Asana project: ${m.asanaProjectGid.ifBlank { "—" }}")
                                    if (m.notes.isNotBlank()) Text("Notes: ${m.notes}")
                                }
                                IconButton(onClick = {
                                    editing = m
                                    showEditor = true
                                }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                IconButton(onClick = {
                                    scope.launch { container.settings.deleteMapping(m.id) }
                                }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                            }
                        }
                    }
                }
            }
        }

        if (showEditor) {
            MappingEditorDialog(
                initial = editing,
                onDismiss = { showEditor = false },
                onSave = { mapping ->
                    scope.launch { container.settings.upsertMapping(mapping) }
                    showEditor = false
                },
            )
        }
    }
}

@Composable
private fun MappingEditorDialog(
    initial: GroupMapping?,
    onDismiss: () -> Unit,
    onSave: (GroupMapping) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var lineId by remember { mutableStateOf(initial?.lineTargetId.orEmpty()) }
    var asanaProject by remember { mutableStateOf(initial?.asanaProjectGid.orEmpty()) }
    var asanaSection by remember { mutableStateOf(initial?.asanaSectionGid.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New mapping" else "Edit mapping") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Label (e.g. Project Athena)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lineId, onValueChange = { lineId = it },
                    label = { Text("LINE target ID (group/room/user)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = asanaProject, onValueChange = { asanaProject = it },
                    label = { Text("Asana project gid") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = asanaSection, onValueChange = { asanaSection = it },
                    label = { Text("Asana section gid (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tip: get a LINE group ID by adding the bot, having someone send a message, and reading the " +
                        "webhook event source.groupId.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isBlank() || lineId.isBlank()) return@TextButton
                onSave(
                    (initial ?: GroupMapping(lineTargetId = lineId, label = label)).copy(
                        label = label.trim(),
                        lineTargetId = lineId.trim(),
                        asanaProjectGid = asanaProject.trim(),
                        asanaSectionGid = asanaSection.trim(),
                        notes = notes.trim(),
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
