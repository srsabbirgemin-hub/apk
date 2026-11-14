package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiProvider
import com.example.data.model.AppSettings
import com.example.ui.AiTestState
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReplyConfigScreen(
    currentSettings: AppSettings,
    aiTestState: AiTestState,
    onTestAi: (AiProvider, String, String, String, String) -> Unit,
    onResetAiTest: () -> Unit,
    onSave: (
        customReply: String,
        aiEnabled: Boolean,
        provider: AiProvider,
        apiKey: String,
        model: String,
        endpointUrl: String,
        systemPrompt: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    DisposableEffect(Unit) {
        onDispose { onResetAiTest() }
    }

    val emptyReplyError = stringResource(R.string.error_empty_reply)
    var customReply by remember(currentSettings.customReplyMessage) {
        mutableStateOf(currentSettings.customReplyMessage)
    }
    var isAiEnabled by remember(currentSettings.isAiEnabled) {
        mutableStateOf(currentSettings.isAiEnabled)
    }
    var selectedProvider by remember(currentSettings.aiProvider) {
        mutableStateOf(currentSettings.aiProvider)
    }
    var apiKey by remember(currentSettings.aiApiKey) {
        mutableStateOf(currentSettings.aiApiKey)
    }
    var modelName by remember(currentSettings.aiModel) {
        mutableStateOf(currentSettings.aiModel)
    }
    var endpointUrl by remember(currentSettings.aiEndpointUrl) {
        mutableStateOf(currentSettings.aiEndpointUrl)
    }
    var systemPrompt by remember(currentSettings.aiSystemPrompt) {
        mutableStateOf(currentSettings.aiSystemPrompt)
    }
    var showApiKey by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Reply & AI Config") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reply_config_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Static / Fallback Auto-Reply Text Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Custom Auto-Reply Message",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Sent when AI replies are off, or used as a safe fallback if an AI call fails.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = customReply,
                            onValueChange = {
                                customReply = it
                                validationError = null
                            },
                            label = { Text("Auto-reply text") },
                            placeholder = { Text("User not available right now...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_reply_input")
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { customReply = "${customReply.trimEnd()} {sender}".trim() },
                                label = { Text("+ {sender}") }
                            )
                            AssistChip(
                                onClick = { customReply = "${customReply.trimEnd()} {app}".trim() },
                                label = { Text("+ {app}") }
                            )
                        }
                    }
                }

                // AI Auto-Replies Configuration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAiEnabled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = "Enable AI Auto-Replies",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Craft smart context-aware replies via OpenAI or DeepSeek",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Checkbox(
                                checked = isAiEnabled,
                                onCheckedChange = { isAiEnabled = it },
                                modifier = Modifier.testTag("enable_ai_checkbox")
                            )
                        }

                        AnimatedVisibility(visible = isAiEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "LLM Provider",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AiProvider.entries.forEach { provider ->
                                        FilterChip(
                                            selected = selectedProvider == provider,
                                            onClick = {
                                                selectedProvider = provider
                                                modelName = provider.defaultModel
                                                endpointUrl = provider.defaultEndpoint
                                                onResetAiTest()
                                            },
                                            label = { Text(provider.displayName) },
                                            leadingIcon = if (selectedProvider == provider) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier.testTag("provider_${provider.name.lowercase()}")
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = {
                                        apiKey = it
                                        validationError = null
                                    },
                                    label = { Text("${selectedProvider.displayName} API Key") },
                                    placeholder = { Text("sk-...") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Key, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Icon(
                                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showApiKey) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    textStyle = TextStyle(fontFamily = JetBrainsMono),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("api_key_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = modelName,
                                        onValueChange = { modelName = it },
                                        label = { Text("Model") },
                                        singleLine = true,
                                        textStyle = TextStyle(fontFamily = JetBrainsMono),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("model_input")
                                    )
                                }

                                OutlinedTextField(
                                    value = endpointUrl,
                                    onValueChange = { endpointUrl = it },
                                    label = { Text("Chat Completions Endpoint URL") },
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = JetBrainsMono),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("endpoint_input")
                                )

                                OutlinedTextField(
                                    value = systemPrompt,
                                    onValueChange = { systemPrompt = it },
                                    label = { Text("Custom AI System Prompt") },
                                    placeholder = { Text("Describe your tone, availability, and instructions...") },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_prompt_input")
                                )

                                OutlinedButton(
                                    onClick = {
                                        onTestAi(selectedProvider, apiKey, modelName, endpointUrl, systemPrompt)
                                    },
                                    enabled = aiTestState !is AiTestState.Loading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_ai_button")
                                ) {
                                    if (aiTestState is AiTestState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Testing LLM Endpoint...")
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Test AI Key & Prompt")
                                    }
                                }

                                when (aiTestState) {
                                    is AiTestState.Success -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Sample Reply Received",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = aiTestState.reply,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                    is AiTestState.Error -> {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            )
                                        ) {
                                            Text(
                                                text = aiTestState.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (customReply.isBlank()) {
                            validationError = emptyReplyError
                            return@Button
                        }
                        onSave(
                            customReply.trim(),
                            isAiEnabled,
                            selectedProvider,
                            apiKey,
                            modelName,
                            endpointUrl,
                            systemPrompt
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_config_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Configuration")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
