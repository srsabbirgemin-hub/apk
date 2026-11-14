package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_apps")
data class TargetAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val shortTag: String,
    val isEnabled: Boolean
)

@Entity(tableName = "contact_rules")
data class ContactRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val note: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reply_templates")
data class ReplyTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val messageBody: String,
    val aiPromptSuggestion: String,
    val isAiTemplate: Boolean,
    val authorHandle: String,
    val usageCount: Int,
    val isFavorite: Boolean = false,
    val isUserCreated: Boolean = false
)

@Entity(tableName = "reply_logs")
data class ReplyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val appName: String,
    val senderName: String,
    val incomingMessage: String,
    val sentReply: String,
    val wasAiGenerated: Boolean,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "contact_cooldowns")
data class ContactCooldownEntity(
    @PrimaryKey val senderKey: String,
    val lastRepliedAt: Long
)
