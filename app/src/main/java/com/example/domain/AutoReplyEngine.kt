package com.example.domain

import com.example.data.local.ReplyLogEntity
import com.example.data.model.AppSettings
import com.example.data.model.ContactFilterMode
import com.example.data.model.ReplyLogStatus
import com.example.data.model.SupportedMessagingApps
import com.example.data.remote.LlmResult
import com.example.data.repository.AutoReplyRepository

data class IncomingMessageEvent(
    val packageName: String,
    val appName: String = SupportedMessagingApps.displayNameFor(packageName),
    val senderName: String,
    val messageText: String,
    val isGroupConversation: Boolean,
    val isSimulation: Boolean = false
)

sealed interface EngineOutcome {
    data class ReplyReady(val replyText: String, val usedAi: Boolean) : EngineOutcome
    data class Skipped(val status: ReplyLogStatus, val reason: String) : EngineOutcome
    data class Failed(val status: ReplyLogStatus, val reason: String) : EngineOutcome
}

class AutoReplyEngine(
    private val repository: AutoReplyRepository,
    private val hasGeneralInternet: () -> Boolean = {
        com.example.service.NetworkStatusTracker.isGeneralInternetAvailable()
    }
) {

    suspend fun evaluateAndPrepareReply(
        event: IncomingMessageEvent,
        hasDirectReplyAction: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): EngineOutcome {
        val settings = repository.getCurrentSettings()

        if (!event.isSimulation && !settings.isServiceEnabled) {
            return EngineOutcome.Skipped(ReplyLogStatus.SKIPPED_CONTACT_FILTER, "Auto-reply master toggle is off.")
        }

        if (!repository.isAppEnabled(event.packageName)) {
            return EngineOutcome.Skipped(ReplyLogStatus.SKIPPED_CONTACT_FILTER, "${event.appName} is not enabled.")
        }

        if (settings.directMessagesOnly && event.isGroupConversation) {
            repository.insertLog(
                ReplyLogEntity(
                    appPackage = event.packageName,
                    appName = event.appName,
                    senderName = event.senderName,
                    incomingMessage = event.messageText,
                    sentReply = "",
                    wasAiGenerated = false,
                    status = ReplyLogStatus.SKIPPED_GROUP.name,
                    timestamp = nowMs
                )
            )
            return EngineOutcome.Skipped(ReplyLogStatus.SKIPPED_GROUP, "Group chat skipped (Direct Messages only).")
        }

        if (!passesContactFilter(settings, event.senderName)) {
            repository.insertLog(
                ReplyLogEntity(
                    appPackage = event.packageName,
                    appName = event.appName,
                    senderName = event.senderName,
                    incomingMessage = event.messageText,
                    sentReply = "",
                    wasAiGenerated = false,
                    status = ReplyLogStatus.SKIPPED_CONTACT_FILTER.name,
                    timestamp = nowMs
                )
            )
            return EngineOutcome.Skipped(
                ReplyLogStatus.SKIPPED_CONTACT_FILTER,
                "Sender '${event.senderName}' excluded by ${settings.contactFilterMode.title} rule."
            )
        }

        val senderKey = "${event.packageName}|${event.senderName.trim().lowercase()}"
        val cooldownMs = settings.replyCooldownSeconds * 1000L
        if (cooldownMs > 0L) {
            val lastRepliedAt = repository.getLastCooldownAt(senderKey)
            if (lastRepliedAt != null && (nowMs - lastRepliedAt) < cooldownMs) {
                val remainingSec = ((cooldownMs - (nowMs - lastRepliedAt)) / 1000L).coerceAtLeast(1L)
                repository.insertLog(
                    ReplyLogEntity(
                        appPackage = event.packageName,
                        appName = event.appName,
                        senderName = event.senderName,
                        incomingMessage = event.messageText,
                        sentReply = "",
                        wasAiGenerated = false,
                        status = ReplyLogStatus.SKIPPED_COOLDOWN.name,
                        timestamp = nowMs
                    )
                )
                return EngineOutcome.Skipped(
                    ReplyLogStatus.SKIPPED_COOLDOWN,
                    "Cooldown active for ${event.senderName} (${remainingSec}s left)."
                )
            }
        }

        if (!hasDirectReplyAction && !event.isSimulation) {
            repository.insertLog(
                ReplyLogEntity(
                    appPackage = event.packageName,
                    appName = event.appName,
                    senderName = event.senderName,
                    incomingMessage = event.messageText,
                    sentReply = "",
                    wasAiGenerated = false,
                    status = ReplyLogStatus.FAILED_NO_ACTION.name,
                    timestamp = nowMs
                )
            )
            return EngineOutcome.Failed(
                ReplyLogStatus.FAILED_NO_ACTION,
                "Notification has no Direct Reply RemoteInput action."
            )
        }

        var usedAi = false
        val shouldTryAi = settings.isAiEnabled &&
            settings.aiApiKey.isNotBlank() &&
            (!settings.instantZeroMbFallback || hasGeneralInternet())

        val replyText = if (shouldTryAi) {
            when (val llm = repository.testAiPrompt(settings, event.senderName, event.appName, event.messageText)) {
                is LlmResult.Success -> {
                    usedAi = true
                    llm.replyText
                }
                is LlmResult.Failure -> {
                    if (settings.fallbackToCustomOnAiError) {
                        formatTemplate(settings.customReplyMessage, event)
                    } else {
                        repository.insertLog(
                            ReplyLogEntity(
                                appPackage = event.packageName,
                                appName = event.appName,
                                senderName = event.senderName,
                                incomingMessage = event.messageText,
                                sentReply = llm.reason,
                                wasAiGenerated = false,
                                status = ReplyLogStatus.FAILED_AI_ERROR.name,
                                timestamp = nowMs
                            )
                        )
                        return EngineOutcome.Failed(ReplyLogStatus.FAILED_AI_ERROR, llm.reason)
                    }
                }
            }
        } else {
            formatTemplate(settings.customReplyMessage, event)
        }

        repository.recordCooldown(senderKey, nowMs)
        repository.insertLog(
            ReplyLogEntity(
                appPackage = event.packageName,
                appName = event.appName,
                senderName = event.senderName,
                incomingMessage = event.messageText,
                sentReply = replyText,
                wasAiGenerated = usedAi,
                status = if (event.isSimulation) ReplyLogStatus.TEST_SIMULATION.name else ReplyLogStatus.SENT.name,
                timestamp = nowMs
            )
        )
        return EngineOutcome.ReplyReady(replyText = replyText, usedAi = usedAi)
    }

    private suspend fun passesContactFilter(settings: AppSettings, senderName: String): Boolean {
        if (settings.contactFilterMode == ContactFilterMode.EVERYONE) return true
        val rules = repository.getAllContactsOnce()
        val normalizedSender = senderName.trim().lowercase()
        val isMatch = rules.any { rule ->
            val target = rule.contactName.trim().lowercase()
            target.isNotEmpty() && (normalizedSender == target || normalizedSender.contains(target))
        }
        return when (settings.contactFilterMode) {
            ContactFilterMode.EVERYONE -> true
            ContactFilterMode.ONLY_SELECTED -> isMatch
            ContactFilterMode.EXCEPT_SELECTED -> !isMatch
        }
    }

    private fun formatTemplate(template: String, event: IncomingMessageEvent): String {
        return template
            .replace("{sender}", event.senderName)
            .replace("{app}", event.appName)
            .trim()
            .ifEmpty { "User not available right now." }
    }
}
