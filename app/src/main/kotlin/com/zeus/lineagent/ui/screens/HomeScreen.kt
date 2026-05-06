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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeus.lineagent.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenSettings: () -> Unit,
    onOpenMappings: () -> Unit,
    onComposeFollowup: (String) -> Unit,
) {
    val mappings by container.settings.mappings.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        initialValue = com.zeus.lineagent.data.AppSettings()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zeus LINE Agent") },
                actions = {
                    IconButton(onClick = onOpenMappings) {
                        Icon(Icons.Filled.Group, contentDescription = "Group mappings")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            val configured = settings.lineToken.isNotBlank()
            if (!configured) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Set up your LINE bot", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Add your LINE channel token in Settings, then map LINE groups to Asana projects.")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Project groups", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (mappings.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No groups yet.")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenMappings) { Text("Add a group mapping") }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mappings, key = { it.id }) { mapping ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(mapping.label, fontWeight = FontWeight.Bold)
                                    Text(
                                        "LINE: ${mapping.lineTargetId.take(12)}…  ·  Asana: ${
                                            if (mapping.asanaProjectGid.isBlank()) "—" else mapping.asanaProjectGid
                                        }",
                                    )
                                }
                                IconButton(onClick = { onComposeFollowup(mapping.id) }) {
                                    Icon(Icons.Filled.Send, contentDescription = "Compose follow-up")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
