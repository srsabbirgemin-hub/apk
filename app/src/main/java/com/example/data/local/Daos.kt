package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetAppDao {
    @Query("SELECT * FROM target_apps ORDER BY isEnabled DESC, appName ASC")
    fun observeAllApps(): Flow<List<TargetAppEntity>>

    @Query("SELECT * FROM target_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): TargetAppEntity?

    @Query("UPDATE target_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setAppEnabled(packageName: String, enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<TargetAppEntity>)
}

@Dao
interface ContactRuleDao {
    @Query("SELECT * FROM contact_rules ORDER BY contactName ASC")
    fun observeContacts(): Flow<List<ContactRuleEntity>>

    @Query("SELECT * FROM contact_rules")
    suspend fun getAllContactsOnce(): List<ContactRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactRuleEntity)

    @Query("DELETE FROM contact_rules WHERE id = :id")
    suspend fun deleteContact(id: Long)

    @Query("DELETE FROM contact_rules")
    suspend fun clearAll()
}

@Dao
interface ReplyTemplateDao {
    @Query("SELECT * FROM reply_templates ORDER BY isFavorite DESC, isUserCreated DESC, usageCount DESC")
    fun observeTemplates(): Flow<List<ReplyTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ReplyTemplateEntity)

    @Query("UPDATE reply_templates SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE reply_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("DELETE FROM reply_templates WHERE id = :id AND isUserCreated = 1")
    suspend fun deleteUserTemplate(id: Long)
}

@Dao
interface ReplyLogDao {
    @Query("SELECT * FROM reply_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeRecentLogs(): Flow<List<ReplyLogEntity>>

    @Query("SELECT COUNT(*) FROM reply_logs WHERE status IN ('SENT', 'TEST_SIMULATION') AND timestamp >= :sinceEpochMs")
    fun observeRepliesSentSince(sinceEpochMs: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReplyLogEntity)

    @Query("DELETE FROM reply_logs")
    suspend fun clearLogs()
}

@Dao
interface ContactCooldownDao {
    @Query("SELECT * FROM contact_cooldowns WHERE senderKey = :senderKey LIMIT 1")
    suspend fun getCooldown(senderKey: String): ContactCooldownEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCooldown(cooldown: ContactCooldownEntity)

    @Query("DELETE FROM contact_cooldowns")
    suspend fun clearCooldowns()
}
