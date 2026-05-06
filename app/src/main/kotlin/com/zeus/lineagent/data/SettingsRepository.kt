package com.zeus.lineagent.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "zeus_settings")

private object Keys {
    val LineToken = stringPreferencesKey("line_channel_access_token")
    val LineSecret = stringPreferencesKey("line_channel_secret")
    val AsanaToken = stringPreferencesKey("asana_pat")
    val AsanaWorkspace = stringPreferencesKey("asana_workspace_gid")
    val ZapierWebhookUrl = stringPreferencesKey("zapier_webhook_url")
    val DisplayName = stringPreferencesKey("user_display_name")
    val Mappings = stringPreferencesKey("group_mappings_json")
}

data class AppSettings(
    val lineToken: String = "",
    val lineSecret: String = "",
    val asanaToken: String = "",
    val asanaWorkspaceGid: String = "",
    val zapierWebhookUrl: String = "",
    val displayName: String = "",
)

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    val mappings: Flow<List<GroupMapping>> = context.dataStore.data.map { prefs ->
        decodeMappings(prefs[Keys.Mappings])
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.LineToken] = updated.lineToken
            prefs[Keys.LineSecret] = updated.lineSecret
            prefs[Keys.AsanaToken] = updated.asanaToken
            prefs[Keys.AsanaWorkspace] = updated.asanaWorkspaceGid
            prefs[Keys.ZapierWebhookUrl] = updated.zapierWebhookUrl
            prefs[Keys.DisplayName] = updated.displayName
        }
    }

    suspend fun upsertMapping(mapping: GroupMapping) {
        context.dataStore.edit { prefs ->
            val current = decodeMappings(prefs[Keys.Mappings]).toMutableList()
            val idx = current.indexOfFirst { it.id == mapping.id }
            if (idx >= 0) current[idx] = mapping else current.add(mapping)
            prefs[Keys.Mappings] = json.encodeToString(GroupMapping.LIST_SERIALIZER, current)
        }
    }

    suspend fun deleteMapping(id: String) {
        context.dataStore.edit { prefs ->
            val current = decodeMappings(prefs[Keys.Mappings]).filter { it.id != id }
            prefs[Keys.Mappings] = json.encodeToString(GroupMapping.LIST_SERIALIZER, current)
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        lineToken = this[Keys.LineToken].orEmpty(),
        lineSecret = this[Keys.LineSecret].orEmpty(),
        asanaToken = this[Keys.AsanaToken].orEmpty(),
        asanaWorkspaceGid = this[Keys.AsanaWorkspace].orEmpty(),
        zapierWebhookUrl = this[Keys.ZapierWebhookUrl].orEmpty(),
        displayName = this[Keys.DisplayName].orEmpty(),
    )

    private fun decodeMappings(raw: String?): List<GroupMapping> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(GroupMapping.LIST_SERIALIZER, raw)
        }.getOrDefault(emptyList())
    }
}
