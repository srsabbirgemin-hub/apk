package com.example

import android.app.Application
import android.content.Context
import com.example.ads.AdStatusTracker
import com.example.ads.AppOpenAdManager
import com.example.data.local.AtomaticDatabase
import com.example.data.local.SettingsPreferencesDataSource
import com.example.data.remote.LlmRepository
import com.example.data.repository.AutoReplyRepository
import com.example.domain.AutoReplyEngine
import com.example.service.NetworkStatusTracker
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database: AtomaticDatabase = AtomaticDatabase.getInstance(context)
    private val settingsDataSource = SettingsPreferencesDataSource(context)
    private val llmRepository = LlmRepository()

    val repository = AutoReplyRepository(
        settingsDataSource = settingsDataSource,
        targetAppDao = database.targetAppDao(),
        contactRuleDao = database.contactRuleDao(),
        replyTemplateDao = database.replyTemplateDao(),
        replyLogDao = database.replyLogDao(),
        contactCooldownDao = database.contactCooldownDao(),
        llmRepository = llmRepository
    )

    val autoReplyEngine = AutoReplyEngine(repository)
}

class AtomaticApplication : Application() {
    lateinit var container: AppContainer
        private set

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NetworkStatusTracker.startMonitoring(this)

        appOpenAdManager = AppOpenAdManager(this).apply {
            register()
        }

        appScope.launch {
            container.repository.ensureDefaultTargetApps()
            runCatching {
                MobileAds.initialize(this@AtomaticApplication) {
                    AdStatusTracker.setSdkInitialized(true)
                    appOpenAdManager.loadAd()
                }
            }.onFailure {
                AdStatusTracker.setBannerStatus("SDK init unavailable: ${it.message}")
                AdStatusTracker.setAppOpenStatus("SDK init unavailable: ${it.message}")
            }
        }
    }
}
