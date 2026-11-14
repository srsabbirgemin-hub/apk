package com.example.ui.components

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ContactRuleEntity
import com.example.data.local.TargetAppEntity
import com.example.data.model.ContactFilterMode
import com.example.ui.SimulationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCooldownLabel(seconds: Int): String = when {
    seconds <= 0 -> "Off"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    else -> "${seconds / 3600}h"
}

fun formatTimestamp(timestampMs: Long): String {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

@Composable
fun ContactRulesDialog(
    filterMode: ContactFilterMode,
    contacts: List<ContactRuleEntity>,
    onAddContact: (String, String) -> Unit,
    onRemoveContact: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (idx >= 0) {
                            val pickedName = cursor.getString(idx).orEmpty()
                            if (pickedName.isNotBlank()) {
                                onAddContact(pickedName, "Picked from Contacts")
                            }
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Contact Filter List",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Active mode: ${filterMode.title} (${filterMode.subtitle})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Contact / Sender Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contact_name_input")
                    )
                    IconButton(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onAddContact(nameInput, noteInput)
                                nameInput = ""
                                noteInput = ""
                            }
                        },
                        modifier = Modifier.testTag("add_contact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add contact rule",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedButton(
                    onClick = { contactPickerLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_device_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Pick from Phone Contacts")
                }

                HorizontalDivider()

                if (contacts.isEmpty()) {
                    Text(
                        text = "No contacts added yet. Add names matching how they appear in notification titles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.contactName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (contact.note.isNotBlank()) {
                                            Text(
                                                text = contact.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onRemoveContact(contact.id) },
                                        modifier = Modifier.testTag("remove_contact_${contact.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove ${contact.contactName}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PipelineSimulatorDialog(
    targetApps: List<TargetAppEntity>,
    simulationState: SimulationState,
    onRunSimulation: (packageName: String, sender: String, message: String, isGroup: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val enabledApps = remember(targetApps) {
        targetApps.filter { it.isEnabled }.ifEmpty { targetApps }
    }
    var selectedPackage by remember(enabledApps) {
        mutableStateOf(enabledApps.firstOrNull()?.packageName ?: "com.whatsapp")
    }
    var senderName by remember { mutableStateOf("Alex Rivera") }
    var messageBody by remember { mutableStateOf("Hey! Are we still on for the meeting at 4 PM?") }
    var isGroup by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Test Reply Pipeline", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Simulate an incoming notification to verify app filters, DM/group rules, cooldowns, and AI/custom text output.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enabledApps.take(5).forEach { app ->
                        FilterChip(
                            selected = selectedPackage == app.packageName,
                            onClick = { selectedPackage = app.packageName },
                            label = { Text(app.appName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sim_sender_input")
                )

                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    label = { Text("Incoming Message") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sim_message_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Simulate Group Chat",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isGroup,
                        onCheckedChange = { isGroup = it },
                        modifier = Modifier.testTag("sim_group_switch")
                    )
                }

                when (simulationState) {
                    SimulationState.Idle -> Unit
                    SimulationState.Running -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Evaluating rules & generating reply...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is SimulationState.Completed -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (simulationState.isSuccess) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (simulationState.isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = simulationState.title,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = simulationState.detail,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRunSimulation(selectedPackage, senderName, messageBody, isGroup) },
                enabled = simulationState !is SimulationState.Running,
                modifier = Modifier.testTag("run_simulation_button")
            ) {
                Text("Run Test")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Close")
            }
        }
    )
}
