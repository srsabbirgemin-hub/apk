package com.example.ads

import android.content.Context
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdStatusTracker {
    private const val TEST_ADAPTIVE_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

    private val _useTestAdsInDebug = MutableStateFlow(BuildConfig.DEBUG)
    val useTestAdsInDebug: StateFlow<Boolean> = _useTestAdsInDebug.asStateFlow()

    private val _bannerStatus = MutableStateFlow("Initializing SDK...")
    val bannerStatus: StateFlow<String> = _bannerStatus.asStateFlow()

    private val _appOpenStatus = MutableStateFlow("Initializing SDK...")
    val appOpenStatus: StateFlow<String> = _appOpenStatus.asStateFlow()

    fun setSdkInitialized(initialized: Boolean) {
        _isSdkInitialized.value = initialized
    }

    fun setUseTestAdsInDebug(useTestAds: Boolean) {
        if (BuildConfig.DEBUG) {
            _useTestAdsInDebug.value = useTestAds
        }
    }

    fun setBannerStatus(status: String) {
        _bannerStatus.value = status
    }

    fun setAppOpenStatus(status: String) {
        _appOpenStatus.value = status
    }

    fun effectiveBannerUnitId(context: Context): String {
        return if (BuildConfig.DEBUG && _useTestAdsInDebug.value) {
            TEST_ADAPTIVE_BANNER_ID
        } else {
            context.getString(R.string.admob_banner_unit_id)
        }
    }

    fun effectiveAppOpenUnitId(context: Context): String {
        return if (BuildConfig.DEBUG && _useTestAdsInDebug.value) {
            TEST_APP_OPEN_ID
        } else {
            context.getString(R.string.admob_app_open_unit_id)
        }
    }
}
