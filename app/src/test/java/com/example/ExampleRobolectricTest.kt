package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ContactFilterMode
import com.example.domain.EngineOutcome
import com.example.domain.IncomingMessageEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Atomatic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Atomatic", appName)
    }

    @Test
    fun `engine enforces dm only, contact filter, and cooldown`() = runTest {
        val app = ApplicationProvider.getApplicationContext<AtomaticApplication>()
        val repo = app.container.repository
        val engine = app.container.autoReplyEngine

        repo.setServiceEnabled(true)
        repo.setAppEnabled("com.whatsapp", true)
        repo.setDirectMessagesOnly(true)
        repo.setReplyCooldownSeconds(60)
        repo.setContactFilterMode(ContactFilterMode.ONLY_SELECTED)
        repo.addContactRule("Jordan")

        // 1. Group message should be skipped
        val groupResult = engine.evaluateAndPrepareReply(
            event = IncomingMessageEvent(
                packageName = "com.whatsapp",
                senderName = "Jordan",
                messageText = "Hello team",
                isGroupConversation = true
            ),
            hasDirectReplyAction = true,
            nowMs = 1_000_000L
        )
        assertTrue(groupResult is EngineOutcome.Skipped)

        // 2. Non-selected contact should be skipped
        val filteredOut = engine.evaluateAndPrepareReply(
            event = IncomingMessageEvent(
                packageName = "com.whatsapp",
                senderName = "Unknown Sender",
                messageText = "Hey",
                isGroupConversation = false
            ),
            hasDirectReplyAction = true,
            nowMs = 1_000_000L
        )
        assertTrue(filteredOut is EngineOutcome.Skipped)

        // 3. Matching DM should succeed
        val sent = engine.evaluateAndPrepareReply(
            event = IncomingMessageEvent(
                packageName = "com.whatsapp",
                senderName = "Jordan",
                messageText = "Are you free?",
                isGroupConversation = false
            ),
            hasDirectReplyAction = true,
            nowMs = 1_000_000L
        )
        assertTrue(sent is EngineOutcome.ReplyReady)

        // 4. Immediate follow-up within 60s should hit cooldown
        val cooldownSkipped = engine.evaluateAndPrepareReply(
            event = IncomingMessageEvent(
                packageName = "com.whatsapp",
                senderName = "Jordan",
                messageText = "Ping again",
                isGroupConversation = false
            ),
            hasDirectReplyAction = true,
            nowMs = 1_020_000L
        )
        assertTrue(cooldownSkipped is EngineOutcome.Skipped)
    }

    @Test
    fun `messenger works in zero-mb mode via instant static fallback`() = runTest {
        val app = ApplicationProvider.getApplicationContext<AtomaticApplication>()
        val repo = app.container.repository
        repo.ensureDefaultTargetApps()
        repo.setServiceEnabled(true)
        repo.setAppEnabled("com.facebook.orca", true)
        repo.setInstantZeroMbFallback(true)
        repo.setReplyCooldownSeconds(0)
        repo.setContactFilterMode(ContactFilterMode.EVERYONE)
        repo.saveReplyAndAiConfig(
            customReplyMessage = "Hi {sender}, busy right now!",
            isAiEnabled = true,
            aiProvider = com.example.data.model.AiProvider.OPENAI,
            aiApiKey = "sk-test-key",
            aiModel = "gpt-4o-mini",
            aiEndpointUrl = "https://api.openai.com/v1/chat/completions",
            aiSystemPrompt = "Be helpful"
        )

        val offlineEngine = com.example.domain.AutoReplyEngine(
            repository = repo,
            hasGeneralInternet = { false }
        )

        val result = offlineEngine.evaluateAndPrepareReply(
            event = IncomingMessageEvent(
                packageName = "com.facebook.orca",
                senderName = "Sabbir",
                messageText = "Kemon acho?",
                isGroupConversation = false
            ),
            hasDirectReplyAction = true,
            nowMs = 2_000_000L
        )

        assertTrue(result is EngineOutcome.ReplyReady)
        val ready = result as EngineOutcome.ReplyReady
        assertEquals(false, ready.usedAi)
        assertEquals("Hi Sabbir, busy right now!", ready.replyText)
    }

    @Test
    fun `main activity launches cleanly`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(!activity.isFinishing)
    }
}
