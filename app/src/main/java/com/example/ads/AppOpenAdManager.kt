package com.example.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.service.NetworkAvailability
import com.example.service.NetworkStatusTracker
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var loadedUnitId: String? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTimeMs: Long = 0L

    private var currentActivity: Activity? = null
    private var startedActivityCount = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    fun register() {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun loadAd(forceReload: Boolean = false) {
        mainHandler.post {
            loadAdOnMainThread(application, forceReload)
        }
    }

    private fun loadAdOnMainThread(context: Context, forceReload: Boolean) {
        if (!AdStatusTracker.isSdkInitialized.value) {
            AdStatusTracker.setAppOpenStatus("Waiting for AdMob SDK...")
            return
        }
        val targetUnitId = AdStatusTracker.effectiveAppOpenUnitId(context)
        if (forceReload || (loadedUnitId != null && loadedUnitId != targetUnitId)) {
            appOpenAd = null
            loadedUnitId = null
        }
        if (isLoadingAd || isAdAvailable()) return
        if (NetworkStatusTracker.availability.value == NetworkAvailability.OFFLINE) {
            AdStatusTracker.setAppOpenStatus("Offline (waiting for internet)")
            return
        }

        isLoadingAd = true
        AdStatusTracker.setAppOpenStatus("Loading...")
        val request = AdRequest.Builder().build()

        runCatching {
            AppOpenAd.load(
                context,
                targetUnitId,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        loadedUnitId = targetUnitId
                        isLoadingAd = false
                        loadTimeMs = Date().time
                        AdStatusTracker.setAppOpenStatus("Ready")
                        Log.d(TAG, "App Open Ad loaded.")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        appOpenAd = null
                        loadedUnitId = null
                        isLoadingAd = false
                        AdStatusTracker.setAppOpenStatus("Failed (${loadAdError.code}): ${loadAdError.message}")
                        Log.d(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    }
                }
            )
        }.onFailure {
            isLoadingAd = false
            AdStatusTracker.setAppOpenStatus("Error: ${it.message}")
            Log.d(TAG, "App Open Ad load skipped: ${it.message}")
        }
    }

    private fun wasLoadTimeLessThanHoursAgo(hours: Long): Boolean {
        val elapsedMs = Date().time - loadTimeMs
        val millisPerHour = 3_600_000L
        return elapsedMs < millisPerHour * hours
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanHoursAgo(4)
    }

    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        if (isShowingAd) {
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }
        if (!isAdAvailable()) {
            onComplete()
            loadAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                loadedUnitId = null
                isShowingAd = false
                onComplete()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                loadedUnitId = null
                isShowingAd = false
                AdStatusTracker.setAppOpenStatus("Show failed: ${adError.message}")
                onComplete()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad shown.")
            }
        }

        isShowingAd = true
        runCatching {
            appOpenAd?.show(activity)
        }.onFailure {
            appOpenAd = null
            loadedUnitId = null
            isShowingAd = false
            onComplete()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        val wasInBackground = startedActivityCount == 0
        startedActivityCount++
        if (!isShowingAd) {
            currentActivity = activity
        }
        val isAdActivity = activity.javaClass.name == "com.google.android.gms.ads.AdActivity"
        if (wasInBackground && !isAdActivity) {
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        if (currentActivity === activity && startedActivityCount == 0) {
            currentActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    companion object {
        private const val TAG = "AppOpenAdManager"
    }
}
