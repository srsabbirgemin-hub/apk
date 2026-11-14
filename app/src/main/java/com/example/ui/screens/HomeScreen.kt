package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ReplyLogEntity
import com.example.data.local.TargetAppEntity
import com.example.data.model.ContactFilterMode
import com.example.data.model.ReplyLogStatus
import com.example.service.NetworkAvailability
import com.example.ui.AtomaticUiState
import com.example.ui.SimulationState
import com.example.ui.components.ContactRulesDialog
import com.example.ui.components.PipelineSimulatorDialog
import com.example.ui.components.formatCooldownLabel
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.HeroDarkBg
import com.example.ui.theme.HeroDarkBorder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: AtomaticUiState,
    simulationState: SimulationState,
    onToggleMaster: (Boolean) -> Unit,
    onOpenReplyConfig: () -> Unit,
    onSetContactFilterMode: (ContactFilterMode) -> Unit,
    onAddContactRule: (String, String) -> Unit,
    onRemoveContactRule: (Long) -> Unit,
    onToggleDirectMessagesOnly: (Boolean) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onSetCooldownSeconds: (Int) -> Unit,
    onRunSimulation: (String, String, String, Boolean) -> Unit,
    onResetSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContactsDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }

    val settings = uiState.settings
    val enabledAppsCount = uiState.targetApps.count { it.isEnabled }
    val cooldownPresets = remember { listOf(0, 30, 60, 300, 900, 1800, 3600) }

    if (showContactsDialog) {
        ContactRulesDialog(
            filterMode = settings.contactFilterMode,
            contacts = uiState.contactRules,
            onAddContact = onAddContactRule,
            onRemoveContact = onRemoveContactRule,
            onDismiss = { showContactsDialog = false }
        )
    }

    if (showSimulatorDialog) {
        PipelineSimulatorDialog(
            targetApps = uiState.targetApps,
            simulationState = simulationState,
            onRunSimulation = onRunSimulation,
            onDismiss = {
                showSimulatorDialog = false
                onResetSimulation()
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner if Notification Access isn't granted
            if (!uiState.serviceStatus.notificationListenerGranted) {
                item {
                    NotificationAccessBanner(
                        onGrantClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Master Hero Card with Master Switch + AI Status Indicator
            item {
                MasterControlHeroCard(
                    isEnabled = settings.isServiceEnabled,
                    isAiEnabled = settings.isAiEnabled,
                    aiProviderName = settings.aiProvider.displayName,
                    hasApiKey = settings.aiApiKey.isNotBlank(),
                    repliesToday = uiState.repliesToday,
                    enabledAppsCount = enabledAppsCount,
                    listenerConnected = uiState.serviceStatus.notificationListenerGranted,
                    networkAvailability = uiState.networkAvailability,
                    onToggleMaster = onToggleMaster,
                    onOpenReplyConfig = onOpenReplyConfig
                )
            }

            // Active Auto-Reply & AI Configuration Preview Card
            item {
                ActiveReplySummaryCard(
                    isAiEnabled = settings.isAiEnabled,
                    aiProviderName = settings.aiProvider.displayName,
                    aiModel = settings.aiModel,
                    customReplyText = settings.customReplyMessage,
                    aiPrompt = settings.aiSystemPrompt,
                    onEditClick = onOpenReplyConfig,
                    onTestClick = { showSimulatorDialog = true }
                )
            }

            // Filter Options Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Filter & Delivery Rules",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // 1. Contacts Filter Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp).size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Contacts Filter", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = settings.contactFilterMode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ContactFilterMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.contactFilterMode == mode,
                                    onClick = { onSetContactFilterMode(mode) },
                                    label = { Text(mode.title) },
                                    leadingIcon = if (settings.contactFilterMode == mode) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("contact_filter_${mode.name.lowercase()}")
                                )
                            }
                        }

                        AnimatedVisibility(visible = settings.contactFilterMode != ContactFilterMode.EVERYONE) {
                            OutlinedButton(
                                onClick = { showContactsDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manage_contacts_button")
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Manage Contact List (${uiState.contactRules.size})")
                            }
                        }
                    }
                }
            }

            // 2. Message Type Card (Direct Messages Only)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDirectMessagesOnly(!settings.directMessagesOnly) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Direct Messages Only", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (settings.directMessagesOnly) {
                                    "Group chats are ignored; only 1-on-1 DMs receive replies"
                                } else {
                                    "Both direct messages and group chats receive auto-replies"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.directMessagesOnly,
                            onCheckedChange = onToggleDirectMessagesOnly,
                            modifier = Modifier.testTag("dm_only_switch")
                        )
                    }
                }
            }

            // 3. Apps Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp).size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Target Messaging Apps", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "$enabledAppsCount of ${uiState.targetApps.size} apps monitored",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.targetApps.forEach { app ->
                                AppSelectorChip(
                                    app = app,
                                    onToggle = { onToggleApp(app.packageName, !app.isEnabled) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Reply Cooldown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(8.dp).size(18.dp)
                                    )
                                }
                                Column {
                                    Text("Reply Cooldown", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (settings.replyCooldownSeconds == 0) {
                                            "Reply immediately to every incoming message"
                                        } else {
                                            "Wait ${formatCooldownLabel(settings.replyCooldownSeconds)} before replying to the same contact again"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = formatCooldownLabel(settings.replyCooldownSeconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cooldownPresets.forEach { seconds ->
                                FilterChip(
                                    selected = settings.replyCooldownSeconds == seconds,
                                    onClick = { onSetCooldownSeconds(seconds) },
                                    label = { Text(formatCooldownLabel(seconds)) },
                                    modifier = Modifier.testTag("cooldown_${seconds}s")
                                )
                            }
                        }
                    }
                }
            }

            // Recent Activity Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${uiState.recentLogs.size} events",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.recentLogs.isEmpty()) {
                item {
                    EmptyActivityCard(onTestClick = { showSimulatorDialog = true })
                }
            } else {
                items(uiState.recentLogs.take(10), key = { it.id }) { log ->
                    ReplyLogCard(log = log)
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun NotificationAccessBanner(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Access Required",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Grant Atomatic permission to read incoming chat notifications and trigger Direct Replies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.testTag("grant_notification_access_button")
                ) {
                    Text("Enable Listener Access")
                }
            }
        }
    }
}

@Composable
private fun MasterControlHeroCard(
    isEnabled: Boolean,
    isAiEnabled: Boolean,
    aiProviderName: String,
    hasApiKey: Boolean,
    repliesToday: Int,
    enabledAppsCount: Int,
    listenerConnected: Boolean,
    networkAvailability: NetworkAvailability,
    onToggleMaster: (Boolean) -> Unit,
    onOpenReplyConfig: () -> Unit
) {
    val activeEmerald = Color(0xFF10B981)
    val aiCyan = Color(0xFF38BDF8)
    val statusDotColor by animateColorAsState(
        targetValue = if (isEnabled) activeEmerald else Color(0xFF64748B),
        label = "statusDot"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = HeroDarkBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) activeEmerald.copy(alpha = 0.45f) else HeroDarkBorder
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = if (isEnabled) {
                            listOf(Color(0xFF0F172A), Color(0xFF064E3B).copy(alpha = 0.55f))
                        } else {
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Text(
                                text = if (isEnabled) "AUTO-REPLY ACTIVE" else "AUTO-REPLY PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEnabled) Color(0xFFA7F3D0) else Color(0xFF94A3B8)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isEnabled) "Atomatic is guarding your inbox" else "Enable to start auto-replying",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleMaster,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = activeEmerald,
                            uncheckedThumbColor = Color(0xFFCBD5E1),
                            uncheckedTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("master_toggle_switch")
                    )
                }

                // AI Status Indicator Pill + Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onOpenReplyConfig,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAiEnabled) aiCyan.copy(alpha = 0.16f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isAiEnabled) aiCyan.copy(alpha = 0.5f) else Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("ai_status_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isAiEnabled) Icons.Default.AutoAwesome else Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "AI reply status",
                                tint = if (isAiEnabled) aiCyan else Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    isAiEnabled && hasApiKey -> "AI Replies: ON ($aiProviderName)"
                                    isAiEnabled -> "AI Replies: Needs API Key"
                                    else -> "AI Replies: OFF (Static)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAiEnabled) Color(0xFFE0F2FE) else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (listenerConnected) activeEmerald else Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (listenerConnected) "Listener Ready" else "Setup Needed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroStatItem(label = "Replies Today", value = repliesToday.toString())
                    HeroStatItem(label = "Active Apps", value = enabledAppsCount.toString())
                    HeroStatItem(
                        label = when (networkAvailability) {
                            NetworkAvailability.ONLINE_VALIDATED -> "MB / Wi-Fi"
                            NetworkAvailability.ZERO_MB_OR_UNVALIDATED -> "0-MB / Data"
                            NetworkAvailability.OFFLINE -> "Local Mode"
                        },
                        value = if (isAiEnabled && networkAvailability == NetworkAvailability.ONLINE_VALIDATED) {
                            "LLM Smart"
                        } else {
                            "Direct Reply"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun ActiveReplySummaryCard(
    isAiEnabled: Boolean,
    aiProviderName: String,
    aiModel: String,
    customReplyText: String,
    aiPrompt: String,
    onEditClick: () -> Unit,
    onTestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isAiEnabled) Icons.Default.AutoAwesome else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isAiEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAiEnabled) "AI Reply Configuration ($aiProviderName · $aiModel)" else "Custom Auto-Reply Text",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAiEnabled) aiPrompt else customReplyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("configure_reply_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Configure Reply & AI")
                }

                OutlinedButton(
                    onClick = onTestClick,
                    modifier = Modifier.testTag("open_simulator_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Test")
                }
            }
        }
    }
}

@Composable
private fun AppSelectorChip(
    app: TargetAppEntity,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = app.isEnabled,
        onClick = onToggle,
        label = { Text(app.appName) },
        leadingIcon = {
            Surface(
                shape = CircleShape,
                color = if (app.isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = app.shortTag.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = if (app.isEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag("app_toggle_${app.packageName}")
    )
}

@Composable
private fun EmptyActivityCard(onTestClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No auto-replies logged yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Incoming notifications from enabled apps or pipeline tests will appear here in real time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onTestClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Run Test Message")
            }
        }
    }
}

@Composable
private fun ReplyLogCard(log: ReplyLogEntity) {
    val statusEnum = ReplyLogStatus.entries.firstOrNull { it.name == log.status } ?: ReplyLogStatus.SENT
    val isSent = statusEnum == ReplyLogStatus.SENT || statusEnum == ReplyLogStatus.TEST_SIMULATION

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = log.senderName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "· ${log.appName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSent) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (log.wasAiGenerated) "AI · ${statusEnum.label}" else statusEnum.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSent) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "In: \"${log.incomingMessage}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (log.sentReply.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = log.sentReply,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatTimestamp(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
