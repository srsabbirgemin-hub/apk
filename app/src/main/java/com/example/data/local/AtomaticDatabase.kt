package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.SupportedMessagingApps

@Database(
    entities = [
        TargetAppEntity::class,
        ContactRuleEntity::class,
        ReplyTemplateEntity::class,
        ReplyLogEntity::class,
        ContactCooldownEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AtomaticDatabase : RoomDatabase() {
    abstract fun targetAppDao(): TargetAppDao
    abstract fun contactRuleDao(): ContactRuleDao
    abstract fun replyTemplateDao(): ReplyTemplateDao
    abstract fun replyLogDao(): ReplyLogDao
    abstract fun contactCooldownDao(): ContactCooldownDao

    companion object {
        @Volatile
        private var INSTANCE: AtomaticDatabase? = null

        fun getInstance(context: Context): AtomaticDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtomaticDatabase::class.java,
                    "atomatic_local.db"
                )
                    .addCallback(SeedDatabaseCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            SupportedMessagingApps.presets.forEach { preset ->
                val values = ContentValues().apply {
                    put("packageName", preset.packageName)
                    put("appName", preset.displayName)
                    put("shortTag", preset.shortTag)
                    put("isEnabled", if (preset.defaultEnabled) 1 else 0)
                }
                db.insert("target_apps", SQLiteDatabase.CONFLICT_IGNORE, values)
            }

            defaultCommunityTemplates().forEach { template ->
                val values = ContentValues().apply {
                    put("title", template.title)
                    put("category", template.category)
                    put("messageBody", template.messageBody)
                    put("aiPromptSuggestion", template.aiPromptSuggestion)
                    put("isAiTemplate", if (template.isAiTemplate) 1 else 0)
                    put("authorHandle", template.authorHandle)
                    put("usageCount", template.usageCount)
                    put("isFavorite", 0)
                    put("isUserCreated", 0)
                }
                db.insert("reply_templates", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }

        private fun defaultCommunityTemplates(): List<ReplyTemplateEntity> = listOf(
            ReplyTemplateEntity(
                title = "Safe Driving Mode",
                category = "Driving",
                messageBody = "Hi {sender}, I'm currently driving and keeping my eyes on the road. I'll reply when I park safely.",
                aiPromptSuggestion = "Let the sender know I am driving right now and cannot check my phone, keep it warm and under 20 words.",
                isAiTemplate = false,
                authorHandle = "@atomatic_core",
                usageCount = 4820
            ),
            ReplyTemplateEntity(
                title = "Deep Focus Sprint",
                category = "Work",
                messageBody = "In a focused work block right now. If urgent, please call twice — otherwise I'll reply in about an hour.",
                aiPromptSuggestion = "Reply politely that I am in a deep focus work block for the next hour and will review their message after.",
                isAiTemplate = false,
                authorHandle = "@productivity_lab",
                usageCount = 3490
            ),
            ReplyTemplateEntity(
                title = "Concise AI Assistant",
                category = "AI Personas",
                messageBody = "Thanks for your message! I'm away from my phone right now, and I'll follow up shortly.",
                aiPromptSuggestion = "You are my executive assistant. Acknowledge the sender's topic specifically in 1 friendly sentence and confirm I will reply soon.",
                isAiTemplate = true,
                authorHandle = "@neural_ops",
                usageCount = 5210
            ),
            ReplyTemplateEntity(
                title = "Client Support Triage AI",
                category = "AI Personas",
                messageBody = "Thanks for reaching out! I'm currently with a client and will respond as soon as I'm free.",
                aiPromptSuggestion = "Act as a polite business concierge. Thank {sender}, briefly acknowledge their request, and ask if it needs urgent scheduling.",
                isAiTemplate = true,
                authorHandle = "@studio_desk",
                usageCount = 2640
            ),
            ReplyTemplateEntity(
                title = "Meeting in Progress",
                category = "Work",
                messageBody = "Hey {sender}, I'm in a meeting on {app} break/mute. Will message you right after!",
                aiPromptSuggestion = "Explain briefly that I am in a meeting right now and will get back to them as soon as it wraps up.",
                isAiTemplate = false,
                authorHandle = "@alex_k",
                usageCount = 2910
            ),
            ReplyTemplateEntity(
                title = "Nighttime Wind-Down",
                category = "Personal",
                messageBody = "Notifications are silenced for the night. I'll catch up with you first thing tomorrow morning!",
                aiPromptSuggestion = "Politely let them know it is late and notifications are paused until morning.",
                isAiTemplate = false,
                authorHandle = "@calm_hours",
                usageCount = 1980
            ),
            ReplyTemplateEntity(
                title = "Out of Office / Travel",
                category = "Personal",
                messageBody = "I'm traveling with limited connectivity right now. I'll check messages periodically and follow up soon.",
                aiPromptSuggestion = "Let the sender know I am traveling with intermittent signal and will reply when back online.",
                isAiTemplate = false,
                authorHandle = "@nomad_flow",
                usageCount = 1750
            )
        )
    }
}
