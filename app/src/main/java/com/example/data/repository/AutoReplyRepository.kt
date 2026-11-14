package com.example.data.repository

import com.example.data.local.ContactCooldownDao
import com.example.data.local.ContactCooldownEntity
import com.example.data.local.ContactRuleDao
import com.example.data.local.ContactRuleEntity
import com.example.data.local.ReplyLogDao
import com.example.data.local.ReplyLogEntity
import com.example.data.local.ReplyTemplateDao
import com.example.data.local.ReplyTemplateEntity
import com.example.data.local.SettingsPreferencesDataSource
import com.example.data.local.TargetAppDao
import com.example.data.local.TargetAppEntity
import com.example.data.model.AiProvider
import com.example.data.model.AppSettings
import com.example.data.model.ContactFilterMode
import com.example.data.model.ThemeMode
import com.example.data.remote.LlmRepository
import com.example.data.remote.LlmResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AutoReplyRepository(
    private val settingsDataSource: SettingsPreferencesDataSource,
    private val targetAppDao: TargetAppDao,
    private val contactRuleDao: ContactRuleDao,
    private val replyTemplateDao: ReplyTemplateDao,
    private val replyLogDao: ReplyLogDao,
    private val contactCooldownDao: ContactCooldownDao,
    private val llmRepository: LlmRepository
) {
    val settingsFlow: Flow<AppSettings> = settingsDataSource.settingsFlow
    val targetAppsFlow: Flow<List<TargetAppEntity>> = targetAppDao.observeAllApps()
    val contactRulesFlow: Flow<List<ContactRuleEntity>> = contactRuleDao.observeContacts()
    val templatesFlow: Flow<List<ReplyTemplateEntity>> = replyTemplateDao.observeTemplates()
    val recentLogsFlow: Flow<List<ReplyLogEntity>> = replyLogDao.observeRecentLogs()

    fun observeRepliesToday(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return replyLogDao.observeRepliesSentSince(cal.timeInMillis)
    }

    suspend fun getCurrentSettings(): AppSettings = settingsFlow.first()

    suspend fun setServiceEnabled(enabled: Boolean) = settingsDataSource.setServiceEnabled(enabled)
    suspend fun setAiEnabled(enabled: Boolean) = settingsDataSource.setAiEnabled(enabled)
    suspend fun setDirectMessagesOnly(dmOnly: Boolean) = settingsDataSource.setDirectMessagesOnly(dmOnly)
    suspend fun setContactFilterMode(mode: ContactFilterMode) = settingsDataSource.setContactFilterMode(mode)
    suspend fun setReplyCooldownSeconds(seconds: Int) = settingsDataSource.setReplyCooldownSeconds(seconds)
    suspend fun setFallbackToCustomOnAiError(enabled: Boolean) =
        settingsDataSource.setFallbackToCustomOnAiError(enabled)
    suspend fun setInstantZeroMbFallback(enabled: Boolean) =
        settingsDataSource.setInstantZeroMbFallback(enabled)
    suspend fun setThemeMode(mode: ThemeMode) = settingsDataSource.setThemeMode(mode)

    suspend fun ensureDefaultTargetApps() {
        val entities = com.example.data.model.SupportedMessagingApps.presets.map { preset ->
            com.example.data.local.TargetAppEntity(
                packageName = preset.packageName,
                appName = preset.displayName,
                shortTag = preset.shortTag,
                isEnabled = preset.defaultEnabled
            )
        }
        targetAppDao.insertApps(entities)
    }

    suspend fun saveReplyAndAiConfig(
        customReplyMessage: String,
        isAiEnabled: Boolean,
        aiProvider: AiProvider,
        aiApiKey: String,
        aiModel: String,
        aiEndpointUrl: String,
        aiSystemPrompt: String
    ) {
        settingsDataSource.updateReplyAndAiConfig(
            customReplyMessage = customReplyMessage,
            isAiEnabled = isAiEnabled,
            aiProvider = aiProvider,
            aiApiKey = aiApiKey,
            aiModel = aiModel,
            aiEndpointUrl = aiEndpointUrl,
            aiSystemPrompt = aiSystemPrompt
        )
    }

    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        targetAppDao.setAppEnabled(packageName, enabled)
    }

    suspend fun isAppEnabled(packageName: String): Boolean {
        return targetAppDao.getApp(packageName)?.isEnabled == true
    }

    suspend fun addContactRule(name: String, note: String = "") {
        val cleaned = name.trim()
        if (cleaned.isNotEmpty()) {
            contactRuleDao.insertContact(ContactRuleEntity(contactName = cleaned, note = note.trim()))
        }
    }

    suspend fun removeContactRule(id: Long) = contactRuleDao.deleteContact(id)

    suspend fun getAllContactsOnce(): List<ContactRuleEntity> = contactRuleDao.getAllContactsOnce()

    suspend fun applyTemplate(template: ReplyTemplateEntity) {
        settingsDataSource.applyTemplate(
            messageBody = template.messageBody,
            aiPromptSuggestion = template.aiPromptSuggestion,
            enableAi = template.isAiTemplate
        )
        replyTemplateDao.incrementUsage(template.id)
    }

    suspend fun toggleTemplateFavorite(template: ReplyTemplateEntity) {
        replyTemplateDao.setFavorite(template.id, !template.isFavorite)
    }

    suspend fun saveUserTemplate(
        title: String,
        category: String,
        messageBody: String,
        aiPromptSuggestion: String,
        isAiTemplate: Boolean
    ) {
        replyTemplateDao.insertTemplate(
            ReplyTemplateEntity(
                title = title.trim(),
                category = category.trim().ifEmpty { "Personal" },
                messageBody = messageBody.trim(),
                aiPromptSuggestion = aiPromptSuggestion.trim(),
                isAiTemplate = isAiTemplate,
                authorHandle = "@you",
                usageCount = 1,
                isFavorite = true,
                isUserCreated = true
            )
        )
    }

    suspend fun deleteUserTemplate(id: Long) = replyTemplateDao.deleteUserTemplate(id)

    suspend fun getLastCooldownAt(senderKey: String): Long? =
        contactCooldownDao.getCooldown(senderKey)?.lastRepliedAt

    suspend fun recordCooldown(senderKey: String, timestamp: Long) {
        contactCooldownDao.upsertCooldown(ContactCooldownEntity(senderKey, timestamp))
    }

    suspend fun clearCooldowns() = contactCooldownDao.clearCooldowns()

    suspend fun insertLog(log: ReplyLogEntity) = replyLogDao.insertLog(log)

    suspend fun clearLogs() = replyLogDao.clearLogs()

    suspend fun testAiPrompt(
        settings: AppSettings,
        sampleSender: String,
        sampleApp: String,
        sampleMessage: String
    ): LlmResult {
        return llmRepository.generateReply(
            settings = settings,
            senderName = sampleSender,
            appName = sampleApp,
            incomingMessage = sampleMessage
        )
    }
}
