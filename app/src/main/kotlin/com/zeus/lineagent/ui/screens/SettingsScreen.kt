package com.zeus.lineagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeus.lineagent.AppContainer
import com.zeus.lineagent.data.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val saved by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var lineToken by remember { mutableStateOf("") }
    var lineSecret by remember { mutableStateOf("") }
    var asanaToken by remember { mutableStateOf("") }
    var asanaWorkspace by remember { mutableStateOf("") }
    var zapierUrl by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var hydrated by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (!hydrated) {
            lineToken = saved.lineToken
            lineSecret = saved.lineSecret
            asanaToken = saved.asanaToken
            asanaWorkspace = saved.asanaWorkspaceGid
            zapierUrl = saved.zapierWebhookUrl
            displayName = saved.displayName
            hydrated = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("LINE Messaging API", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Your display name (used in messages)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lineToken,
                onValueChange = { lineToken = it },
                label = { Text("Channel access token (long-lived)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lineSecret,
                onValueChange = { lineSecret = it },
                label = { Text("Channel secret (optional, for webhook verification)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Text("Asana", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = asanaToken,
                onValueChange = { asanaToken = it },
                label = { Text("Personal access token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = asanaWorkspace,
                onValueChange = { asanaWorkspace = it },
                label = { Text("Workspace gid (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Text("Zapier", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = zapierUrl,
                onValueChange = { zapierUrl = it },
                label = { Text("Zapier Catch Hook URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        container.settings.updateSettings {
                            it.copy(
                                lineToken = lineToken.trim(),
                                lineSecret = lineSecret.trim(),
                                asanaToken = asanaToken.trim(),
                                asanaWorkspaceGid = asanaWorkspace.trim(),
                                zapierWebhookUrl = zapierUrl.trim(),
                                displayName = displayName.trim(),
                            )
                        }
                        snackbar.showSnackbar("Saved")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}
