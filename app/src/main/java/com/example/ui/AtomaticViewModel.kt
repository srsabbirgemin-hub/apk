package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AtomaticApplication
import com.example.data.local.ContactRuleEntity
import com.example.data.local.ReplyLogEntity
import com.example.data.local.ReplyTemplateEntity
import com.example.data.local.TargetAppEntity
import com.example.data.model.AiProvider
import com.example.data.model.AppSettings
import com.example.data.model.ContactFilterMode
import com.example.data.model.ThemeMode
import com.example.data.remote.LlmResult
import com.example.data.repository.AutoReplyRepository
import com.example.domain.AutoReplyEngine
import com.example.domain.EngineOutcome
import com.example.domain.IncomingMessageEvent
import com.example.service.NetworkAvailability
import com.example.service.NetworkStatusTracker
import com.example.service.ServiceConnectionStatus
import com.example.service.ServiceStatusTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AtomaticUiState(
    val settings: AppSettings = AppSettings(),
    val targetApps: List<TargetAppEntity> = emptyList(),
    val contactRules: List<ContactRuleEntity> = emptyList(),
    val templates: List<ReplyTemplateEntity> = emptyList(),
    val recentLogs: List<ReplyLogEntity> = emptyList(),
    val repliesToday: Int = 0,
    val serviceStatus: ServiceConnectionStatus = ServiceConnectionStatus(),
    val networkAvailability: NetworkAvailability = NetworkAvailability.OFFLINE
)

sealed interface AiTestState {
    data object Idle : AiTestState
    data object Loading : AiTestState
    data class Success(val reply: String) : AiTestState
    data class Error(val message: String) : AiTestState
}

sealed interface SimulationState {
    data object Idle : SimulationState
    data object Running : SimulationState
    data class Completed(val title: String, val detail: String, val isSuccess: Boolean) : SimulationState
}

class AtomaticViewModel(
    application: Application,
    private val repository: AutoReplyRepository,
    private val engine: AutoReplyEngine
) : AndroidViewModel(application) {

    val uiState: StateFlow<AtomaticUiState> = combine(
        combine(
            repository.settingsFlow,
            repository.targetAppsFlow,
            repository.contactRulesFlow,
            repository.templatesFlow
        ) { settings, apps, contacts, templates ->
            Quadruple(settings, apps, contacts, templates)
        },
        combine(
            repository.recentLogsFlow,
            repository.observeRepliesToday(),
            ServiceStatusTracker.status,
            NetworkStatusTracker.availability
        ) { logs, countToday, status, network ->
            Quadruple(logs, countToday, status, network)
        }
    ) { left, right ->
        AtomaticUiState(
            settings = left.first,
            targetApps = left.second,
            contactRules = left.third,
            templates = left.fourth,
            recentLogs = right.first,
            repliesToday = right.second,
            serviceStatus = right.third,
            networkAvailability = right.fourth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AtomaticUiState()
    )

    private val _aiTestState = MutableStateFlow<AiTestState>(AiTestState.Idle)
    val aiTestState: StateFlow<AiTestState> = _aiTestState.asStateFlow()

    private val _simulationState = MutableStateFlow<SimulationState>(SimulationState.Idle)
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    fun refreshServiceStatus() {
        ServiceStatusTracker.refresh(getApplication())
        NetworkStatusTracker.refresh(getApplication())
    }

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setServiceEnabled(enabled) }
    }

    fun setDirectMessagesOnly(dmOnly: Boolean) {
        viewModelScope.launch { repository.setDirectMessagesOnly(dmOnly) }
    }

    fun setContactFilterMode(mode: ContactFilterMode) {
        viewModelScope.launch { repository.setContactFilterMode(mode) }
    }

    fun setReplyCooldownSeconds(seconds: Int) {
        viewModelScope.launch { repository.setReplyCooldownSeconds(seconds) }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.setAppEnabled(packageName, enabled) }
    }

    fun addContactRule(name: String, note: String = "") {
        viewModelScope.launch { repository.addContactRule(name, note) }
    }

    fun removeContactRule(id: Long) {
        viewModelScope.launch { repository.removeContactRule(id) }
    }

    fun saveReplyAndAiConfig(
        customReplyMessage: String,
        isAiEnabled: Boolean,
        aiProvider: AiProvider,
        aiApiKey: String,
        aiModel: String,
        aiEndpointUrl: String,
        aiSystemPrompt: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveReplyAndAiConfig(
                customReplyMessage = customReplyMessage,
                isAiEnabled = isAiEnabled,
                aiProvider = aiProvider,
                aiApiKey = aiApiKey,
                aiModel = aiModel,
                aiEndpointUrl = aiEndpointUrl,
                aiSystemPrompt = aiSystemPrompt
            )
            onSaved()
        }
    }

    fun testAiConfiguration(
        aiProvider: AiProvider,
        aiApiKey: String,
        aiModel: String,
        aiEndpointUrl: String,
        aiSystemPrompt: String
    ) {
        viewModelScope.launch {
            _aiTestState.value = AiTestState.Loading
            val testSettings = uiState.value.settings.copy(
                isAiEnabled = true,
                aiProvider = aiProvider,
                aiApiKey = aiApiKey.trim(),
                aiModel = aiModel.trim().ifEmpty { aiProvider.defaultModel },
                aiEndpointUrl = aiEndpointUrl.trim().ifEmpty { aiProvider.defaultEndpoint },
                aiSystemPrompt = aiSystemPrompt.trim()
            )
            when (val result = repository.testAiPrompt(
                settings = testSettings,
                sampleSender = "Jordan",
                sampleApp = "WhatsApp",
                sampleMessage = "Hey! Are you free for a quick call around 3 PM?"
            )) {
                is LlmResult.Success -> _aiTestState.value = AiTestState.Success(result.replyText)
                is LlmResult.Failure -> _aiTestState.value = AiTestState.Error(result.reason)
            }
        }
    }

    fun resetAiTestState() {
        _aiTestState.value = AiTestState.Idle
    }

    fun applyTemplate(template: ReplyTemplateEntity, onApplied: () -> Unit) {
        viewModelScope.launch {
            repository.applyTemplate(template)
            onApplied()
        }
    }

    fun toggleTemplateFavorite(template: ReplyTemplateEntity) {
        viewModelScope.launch { repository.toggleTemplateFavorite(template) }
    }

    fun createUserTemplate(
        title: String,
        category: String,
        messageBody: String,
        aiPromptSuggestion: String,
        isAiTemplate: Boolean
    ) {
        viewModelScope.launch {
            repository.saveUserTemplate(
                title = title,
                category = category,
                messageBody = messageBody,
                aiPromptSuggestion = aiPromptSuggestion,
                isAiTemplate = isAiTemplate
            )
        }
    }

    fun deleteUserTemplate(id: Long) {
        viewModelScope.launch { repository.deleteUserTemplate(id) }
    }

    fun setFallbackToCustomOnAiError(enabled: Boolean) {
        viewModelScope.launch { repository.setFallbackToCustomOnAiError(enabled) }
    }

    fun setInstantZeroMbFallback(enabled: Boolean) {
        viewModelScope.launch { repository.setInstantZeroMbFallback(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun clearCooldowns() {
        viewModelScope.launch { repository.clearCooldowns() }
    }

    fun clearLogs() {
        viewModelScope.launch { repository.clearLogs() }
    }

    fun runPipelineSimulation(
        packageName: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean
    ) {
        viewModelScope.launch {
            _simulationState.value = SimulationState.Running
            val outcome = engine.evaluateAndPrepareReply(
                event = IncomingMessageEvent(
                    packageName = packageName,
                    senderName = senderName.trim().ifEmpty { "Alex" },
                    messageText = messageText.trim().ifEmpty { "Hey, are you available?" },
                    isGroupConversation = isGroup,
                    isSimulation = true
                ),
                hasDirectReplyAction = true
            )
            _simulationState.value = when (outcome) {
                is EngineOutcome.ReplyReady -> SimulationState.Completed(
                    title = if (outcome.usedAi) "AI Reply Dispatched" else "Auto-Reply Dispatched",
                    detail = outcome.replyText,
                    isSuccess = true
                )
                is EngineOutcome.Skipped -> SimulationState.Completed(
                    title = outcome.status.label,
                    detail = outcome.reason,
                    isSuccess = false
                )
                is EngineOutcome.Failed -> SimulationState.Completed(
                    title = outcome.status.label,
                    detail = outcome.reason,
                    isSuccess = false
                )
            }
        }
    }

    fun resetSimulationState() {
        _simulationState.value = SimulationState.Idle
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val container = (application as AtomaticApplication).container
                    return AtomaticViewModel(
                        application = application,
                        repository = container.repository,
                        engine = container.autoReplyEngine
                    ) as T
                }
            }
    }
}
