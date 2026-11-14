package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AtomaticApplication
import com.example.BuildConfig
import com.example.ads.AdStatusTracker
import com.example.data.model.ThemeMode
import com.example.ui.AtomaticUiState
import com.example.ui.SimulationState
import com.example.ui.components.PipelineSimulatorDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: AtomaticUiState,
    simulationState: SimulationState,
    onToggleFallback: (Boolean) -> Unit,
    onToggleInstantZeroMbFallback: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onClearCooldowns: () -> Unit,
    onClearLogs: () -> Unit,
    onOpenReplyConfig: () -> Unit,
    onRunSimulation: (String, String, String, Boolean) -> Unit,
    onResetSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSimulator by remember { mutableStateOf(false) }

    var postNotificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        postNotificationsGranted = granted
    }

    if (showSimulator) {
        PipelineSimulatorDialog(
            targetApps = uiState.targetApps,
            simulationState = simulationState,
            onRunSimulation = onRunSimulation,
            onDismiss = {
                showSimulator = false
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
            item {
                Text("Settings & Services", style = MaterialTheme.typography.headlineMedium)
            }

            // System Services & Permissions Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Android Background Services", style = MaterialTheme.typography.titleMedium)

                        ServiceStatusRow(
                            icon = Icons.Default.Notifications,
                            title = "Notification Listener Service",
                            subtitle = "Reads incoming messaging alerts & sends RemoteInput Direct Replies.",
                            isGranted = uiState.serviceStatus.notificationListenerGranted,
                            actionLabel = "Open Notification Access",
                            buttonTestTag = "open_notification_settings_button",
                            onClick = {
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

                        ServiceStatusRow(
                            icon = Icons.Default.AccessibilityNew,
                            title = "Accessibility Reply Helper",
                            subtitle = "Optional helper for monitoring notification state transitions.",
                            isGranted = uiState.serviceStatus.accessibilityServiceEnabled,
                            actionLabel = "Open Accessibility Settings",
                            buttonTestTag = "open_accessibility_settings_button",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
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

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !postNotificationsGranted) {
                            OutlinedButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Allow Status Notifications (Android 13+)")
                            }
                        }
                    }
                }
            }

            // Reply Safety & AI Fallback Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Reliability & Safety", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Fallback to Custom Text on AI Error",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "If LLM API times out or key is missing, reply with your static message.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.fallbackToCustomOnAiError,
                                onCheckedChange = onToggleFallback,
                                modifier = Modifier.testTag("fallback_ai_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Instant 0-MB / Zero-Rated Mode",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "When MB is exhausted (e.g., Free Messenger data), skip AI calls and send static Direct Replies immediately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.instantZeroMbFallback,
                                onCheckedChange = onToggleInstantZeroMbFallback,
                                modifier = Modifier.testTag("zero_mb_fallback_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenReplyConfig,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Auto-Reply Text & LLM API Key")
                        }
                    }
                }
            }

            // Appearance Card
            item {
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Storage & Diagnostics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Diagnostics & Local Data", style = MaterialTheme.typography.titleMedium)

                        Button(
                            onClick = { showSimulator = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Launch Pipeline Simulator")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClearCooldowns,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset Cooldowns")
                            }
                            OutlinedButton(
                                onClick = onClearLogs,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear Logs")
                            }
                        }
                    }
                }
            }

            // AdMob Status & Diagnostics Card
            item {
                val bannerStatus by AdStatusTracker.bannerStatus.collectAsStateWithLifecycle()
                val appOpenStatus by AdStatusTracker.appOpenStatus.collectAsStateWithLifecycle()
                val useTestAds by AdStatusTracker.useTestAdsInDebug.collectAsStateWithLifecycle()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("AdMob Ads Status", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Banner: $bannerStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "App Open: $appOpenStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (BuildConfig.DEBUG) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Use Google Test Ads (Debug)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Shows instant sample ads while new AdMob units propagate (~1 hr). Release builds always use live IDs.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = useTestAds,
                                    onCheckedChange = { checked ->
                                        AdStatusTracker.setUseTestAdsInDebug(checked)
                                        (context.applicationContext as? AtomaticApplication)
                                            ?.appOpenAdManager
                                            ?.loadAd(forceReload = true)
                                    },
                                    modifier = Modifier.testTag("use_test_ads_switch")
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val app = context.applicationContext as? AtomaticApplication
                                val activity = context as? android.app.Activity
                                if (app != null && activity != null) {
                                    app.appOpenAdManager.showAdIfAvailable(activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Preview / Reload App Open Ad")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ServiceStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    buttonTestTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleSmall)
                }
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Enabled" else "Not enabled",
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(buttonTestTag)
            ) {
                Text(if (isGranted) "Manage in System Settings" else actionLabel)
            }
        }
    }
}
