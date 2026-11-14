package com.example.data.model

enum class ContactFilterMode(val title: String, val subtitle: String) {
    EVERYONE("Everyone", "Reply to all contacts"),
    ONLY_SELECTED("Only Selected", "Reply only to listed contacts"),
    EXCEPT_SELECTED("Except Selected", "Skip listed contacts")
}

enum class AiProvider(
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String
) {
    OPENAI(
        displayName = "OpenAI",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultEndpoint = "https://api.deepseek.com/chat/completions",
        defaultModel = "deepseek-chat"
    ),
    CUSTOM(
        displayName = "Custom API",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini"
    )
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class ReplyLogStatus(val label: String) {
    SENT("Auto-Replied"),
    TEST_SIMULATION("Test Reply"),
    SKIPPED_COOLDOWN("Skipped (Cooldown)"),
    SKIPPED_GROUP("Skipped (Group)"),
    SKIPPED_CONTACT_FILTER("Skipped (Filter)"),
    FAILED_NO_ACTION("No Reply Action"),
    FAILED_AI_ERROR("AI Request Failed")
}

data class AppSettings(
    val isServiceEnabled: Boolean = false,
    val isAiEnabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.OPENAI,
    val aiApiKey: String = "",
    val aiModel: String = AiProvider.OPENAI.defaultModel,
    val aiEndpointUrl: String = AiProvider.OPENAI.defaultEndpoint,
    val aiSystemPrompt: String = "You are a polite, concise personal assistant replying on my behalf. Acknowledge the message naturally in 1–2 sentences and let the sender know I will follow up shortly.",
    val customReplyMessage: String = "Hi! I am currently unavailable. I will get back to you as soon as possible.",
    val directMessagesOnly: Boolean = true,
    val contactFilterMode: ContactFilterMode = ContactFilterMode.EVERYONE,
    val replyCooldownSeconds: Int = 300,
    val fallbackToCustomOnAiError: Boolean = true,
    val instantZeroMbFallback: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

data class MessagingAppPreset(
    val packageName: String,
    val displayName: String,
    val shortTag: String,
    val defaultEnabled: Boolean
)

object SupportedMessagingApps {
    val presets: List<MessagingAppPreset> = listOf(
        MessagingAppPreset("com.whatsapp", "WhatsApp", "WA", true),
        MessagingAppPreset("com.whatsapp.w4b", "WhatsApp Business", "WB", false),
        MessagingAppPreset("org.telegram.messenger", "Telegram", "TG", true),
        MessagingAppPreset("com.facebook.orca", "Messenger", "MS", true),
        MessagingAppPreset("com.facebook.mlite", "Messenger Lite", "ML", true),
        MessagingAppPreset("com.instagram.android", "Instagram", "IG", false),
        MessagingAppPreset("org.thoughtcrime.securesms", "Signal", "SG", false),
        MessagingAppPreset("com.google.android.apps.messaging", "Messages", "SMS", false),
        MessagingAppPreset("com.Slack", "Slack", "SL", false)
    )

    fun displayNameFor(packageName: String): String {
        return presets.firstOrNull { it.packageName == packageName }?.displayName
            ?: packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}
