package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiProvider
import com.example.data.model.AppSettings
import com.example.data.model.ContactFilterMode
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.atomaticDataStore: DataStore<Preferences> by preferencesDataStore(name = "atomatic_settings")

class SettingsPreferencesDataSource(private val context: Context) {

    private object Keys {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_ENDPOINT_URL = stringPreferencesKey("ai_endpoint_url")
        val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val CUSTOM_REPLY_MESSAGE = stringPreferencesKey("custom_reply_message")
        val DIRECT_MESSAGES_ONLY = booleanPreferencesKey("direct_messages_only")
        val CONTACT_FILTER_MODE = stringPreferencesKey("contact_filter_mode")
        val REPLY_COOLDOWN_SECONDS = intPreferencesKey("reply_cooldown_seconds")
        val FALLBACK_ON_AI_ERROR = booleanPreferencesKey("fallback_on_ai_error")
        val INSTANT_ZERO_MB_FALLBACK = booleanPreferencesKey("instant_zero_mb_fallback")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.atomaticDataStore.data.map { prefs ->
        val defaultSettings = AppSettings()
        val provider = prefs[Keys.AI_PROVIDER]?.let { name ->
            AiProvider.entries.firstOrNull { it.name == name }
        } ?: defaultSettings.aiProvider

        val filterMode = prefs[Keys.CONTACT_FILTER_MODE]?.let { name ->
            ContactFilterMode.entries.firstOrNull { it.name == name }
        } ?: defaultSettings.contactFilterMode

        val themeMode = prefs[Keys.THEME_MODE]?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: defaultSettings.themeMode

        AppSettings(
            isServiceEnabled = prefs[Keys.SERVICE_ENABLED] ?: defaultSettings.isServiceEnabled,
            isAiEnabled = prefs[Keys.AI_ENABLED] ?: defaultSettings.isAiEnabled,
            aiProvider = provider,
            aiApiKey = prefs[Keys.AI_API_KEY] ?: defaultSettings.aiApiKey,
            aiModel = prefs[Keys.AI_MODEL] ?: provider.defaultModel,
            aiEndpointUrl = prefs[Keys.AI_ENDPOINT_URL] ?: provider.defaultEndpoint,
            aiSystemPrompt = prefs[Keys.AI_SYSTEM_PROMPT] ?: defaultSettings.aiSystemPrompt,
            customReplyMessage = prefs[Keys.CUSTOM_REPLY_MESSAGE] ?: defaultSettings.customReplyMessage,
            directMessagesOnly = prefs[Keys.DIRECT_MESSAGES_ONLY] ?: defaultSettings.directMessagesOnly,
            contactFilterMode = filterMode,
            replyCooldownSeconds = prefs[Keys.REPLY_COOLDOWN_SECONDS] ?: defaultSettings.replyCooldownSeconds,
            fallbackToCustomOnAiError = prefs[Keys.FALLBACK_ON_AI_ERROR] ?: defaultSettings.fallbackToCustomOnAiError,
            instantZeroMbFallback = prefs[Keys.INSTANT_ZERO_MB_FALLBACK] ?: defaultSettings.instantZeroMbFallback,
            themeMode = themeMode
        )
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.atomaticDataStore.edit { it[Keys.SERVICE_ENABLED] = enabled }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.atomaticDataStore.edit { it[Keys.AI_ENABLED] = enabled }
    }

    suspend fun setDirectMessagesOnly(dmOnly: Boolean) {
        context.atomaticDataStore.edit { it[Keys.DIRECT_MESSAGES_ONLY] = dmOnly }
    }

    suspend fun setContactFilterMode(mode: ContactFilterMode) {
        context.atomaticDataStore.edit { it[Keys.CONTACT_FILTER_MODE] = mode.name }
    }

    suspend fun setReplyCooldownSeconds(seconds: Int) {
        context.atomaticDataStore.edit { it[Keys.REPLY_COOLDOWN_SECONDS] = seconds.coerceAtLeast(0) }
    }

    suspend fun setFallbackToCustomOnAiError(enabled: Boolean) {
        context.atomaticDataStore.edit { it[Keys.FALLBACK_ON_AI_ERROR] = enabled }
    }

    suspend fun setInstantZeroMbFallback(enabled: Boolean) {
        context.atomaticDataStore.edit { it[Keys.INSTANT_ZERO_MB_FALLBACK] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.atomaticDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateReplyAndAiConfig(
        customReplyMessage: String,
        isAiEnabled: Boolean,
        aiProvider: AiProvider,
        aiApiKey: String,
        aiModel: String,
        aiEndpointUrl: String,
        aiSystemPrompt: String
    ) {
        context.atomaticDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_REPLY_MESSAGE] = customReplyMessage
            prefs[Keys.AI_ENABLED] = isAiEnabled
            prefs[Keys.AI_PROVIDER] = aiProvider.name
            prefs[Keys.AI_API_KEY] = aiApiKey.trim()
            prefs[Keys.AI_MODEL] = aiModel.trim().ifEmpty { aiProvider.defaultModel }
            prefs[Keys.AI_ENDPOINT_URL] = aiEndpointUrl.trim().ifEmpty { aiProvider.defaultEndpoint }
            prefs[Keys.AI_SYSTEM_PROMPT] = aiSystemPrompt.trim()
        }
    }

    suspend fun applyTemplate(
        messageBody: String,
        aiPromptSuggestion: String,
        enableAi: Boolean
    ) {
        context.atomaticDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_REPLY_MESSAGE] = messageBody
            if (aiPromptSuggestion.isNotBlank()) {
                prefs[Keys.AI_SYSTEM_PROMPT] = aiPromptSuggestion
            }
            prefs[Keys.AI_ENABLED] = enableAi
        }
    }
}
