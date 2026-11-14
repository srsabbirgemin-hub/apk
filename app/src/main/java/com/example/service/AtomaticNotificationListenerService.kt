package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AtomaticApplication
import com.example.domain.EngineOutcome
import com.example.domain.IncomingMessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Collections

class AtomaticNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentSignatures: MutableMap<String, Long> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 64
            }
        }
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        ServiceStatusTracker.setNotificationListenerConnected(true)
        ServiceStatusTracker.refresh(applicationContext)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ServiceStatusTracker.setNotificationListenerConnected(false)
        ServiceStatusTracker.refresh(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val packageName = statusBarNotification.packageName ?: return
        if (packageName == applicationContext.packageName) return

        val notification = statusBarNotification.notification ?: return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val replyAction = extractDirectReplyAction(notification)
        val parsed = parseIncomingMessage(packageName, notification) ?: return

        val signature = "${statusBarNotification.key}|${parsed.senderName}|${parsed.messageText}|${notification.`when`}"
        val now = System.currentTimeMillis()
        synchronized(recentSignatures) {
            val previousAt = recentSignatures[signature]
            if (previousAt != null && (now - previousAt) < 15_000L) {
                return
            }
            recentSignatures[signature] = now
        }

        val appContainer = (application as AtomaticApplication).container
        serviceScope.launch {
            val outcome = appContainer.autoReplyEngine.evaluateAndPrepareReply(
                event = parsed,
                hasDirectReplyAction = replyAction != null
            )

            if (outcome is EngineOutcome.ReplyReady && replyAction != null) {
                sendDirectReply(replyAction, outcome.replyText)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun parseIncomingMessage(
        packageName: String,
        notification: Notification
    ): IncomingMessageEvent? {
        val extras = notification.extras ?: return null
        val messagingStyle = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)

        val latestMessage = messagingStyle?.messages?.lastOrNull()
        if (messagingStyle != null && latestMessage != null) {
            val messageSender = latestMessage.person
            val localUser = messagingStyle.user
            val isSelfMessage = messageSender == null ||
                (!localUser.key.isNullOrBlank() && messageSender.key == localUser.key) ||
                (!localUser.name.isNullOrBlank() && messageSender.name?.toString() == localUser.name?.toString())
            if (isSelfMessage) return null
        }

        val sender = latestMessage?.person?.name?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: return null

        val messageText = latestMessage?.text?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return null

        if (sender.isBlank() || messageText.isBlank()) return null

        val isGroup = extras.getBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION, false) ||
            (messagingStyle?.isGroupConversation == true) ||
            !extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE).isNullOrBlank()

        return IncomingMessageEvent(
            packageName = packageName,
            senderName = sender.trim(),
            messageText = messageText.trim(),
            isGroupConversation = isGroup,
            isSimulation = false
        )
    }

    private fun extractDirectReplyAction(notification: Notification): DirectReplyAction? {
        notification.actions?.forEach { action ->
            val remoteInputs = action.remoteInputs
            if (action.actionIntent != null && remoteInputs != null && remoteInputs.isNotEmpty()) {
                return DirectReplyAction(action.actionIntent, remoteInputs)
            }
        }
        val wearableActions = NotificationCompat.WearableExtender(notification).actions
        wearableActions.forEach { compatAction ->
            val compatInputs = compatAction.remoteInputs
            val intent = compatAction.actionIntent
            if (intent != null && compatInputs != null && compatInputs.isNotEmpty()) {
                val frameworkInputs = compatInputs.map { input ->
                    RemoteInput.Builder(input.resultKey)
                        .setLabel(input.label)
                        .setChoices(input.choices)
                        .setAllowFreeFormInput(input.allowFreeFormInput)
                        .addExtras(input.extras)
                        .build()
                }.toTypedArray()
                return DirectReplyAction(intent, frameworkInputs)
            }
        }
        return null
    }

    private fun sendDirectReply(action: DirectReplyAction, replyText: String) {
        try {
            val replyIntent = Intent()
            val bundle = Bundle()
            action.remoteInputs.forEach { remoteInput ->
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(action.remoteInputs, replyIntent, bundle)
            action.pendingIntent.send(applicationContext, 0, replyIntent)
        } catch (e: Exception) {
            Log.e("AtomaticListener", "Failed sending Direct Reply action", e)
        }
    }

    private data class DirectReplyAction(
        val pendingIntent: android.app.PendingIntent,
        val remoteInputs: Array<RemoteInput>
    )
}
