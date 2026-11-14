package com.example.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdStatusTracker
import com.example.service.NetworkAvailability
import com.example.service.NetworkStatusTracker
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
) {
    val isInPreview = LocalInspectionMode.current

    if (isInPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AdMob Banner Preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isSdkInitialized by AdStatusTracker.isSdkInitialized.collectAsStateWithLifecycle()
    val useTestAds by AdStatusTracker.useTestAdsInDebug.collectAsStateWithLifecycle()
    val networkAvailability by NetworkStatusTracker.availability.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("admob_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val adWidthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val bannerUnitId = remember(useTestAds) { AdStatusTracker.effectiveBannerUnitId(context) }

        val adaptiveSize = remember(adWidthDp) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidthDp)
        }
        val bannerHeightDp = remember(adaptiveSize) {
            adaptiveSize.height.coerceAtLeast(50).dp
        }

        var hasLoadedAd by remember(bannerUnitId) { mutableStateOf(false) }

        val adView = remember(bannerUnitId, adaptiveSize) {
            AdView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adUnitId = bannerUnitId
                setAdSize(adaptiveSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        hasLoadedAd = true
                        AdStatusTracker.setBannerStatus("Loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        hasLoadedAd = false
                        AdStatusTracker.setBannerStatus("Failed (${error.code}): ${error.message}")
                    }
                }
            }
        }

        LaunchedEffect(adView, isSdkInitialized, networkAvailability) {
            if (isSdkInitialized && networkAvailability != NetworkAvailability.OFFLINE && !hasLoadedAd) {
                AdStatusTracker.setBannerStatus("Loading...")
                runCatching {
                    adView.loadAd(AdRequest.Builder().build())
                }.onFailure {
                    AdStatusTracker.setBannerStatus("Error: ${it.message}")
                }
            } else if (networkAvailability == NetworkAvailability.OFFLINE && !hasLoadedAd) {
                AdStatusTracker.setBannerStatus("Offline (waiting for internet)")
            }
        }

        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> runCatching { adView.resume() }
                    Lifecycle.Event.ON_PAUSE -> runCatching { adView.pause() }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                runCatching { adView.destroy() }
            }
        }

        val shouldShowSlot = hasLoadedAd || (isSdkInitialized && networkAvailability != NetworkAvailability.OFFLINE)

        Column(modifier = Modifier.fillMaxWidth()) {
            if (shouldShowSlot) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            key(adView) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (shouldShowSlot) bannerHeightDp else 0.dp)
                )
            }
        }
    }
}
