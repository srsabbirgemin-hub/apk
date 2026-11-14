package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkAvailability(val label: String) {
    ONLINE_VALIDATED("MB / Wi-Fi Online"),
    ZERO_MB_OR_UNVALIDATED("0-MB / Restricted Data"),
    OFFLINE("Offline · Local Only")
}

object NetworkStatusTracker {
    private val _availability = MutableStateFlow(NetworkAvailability.OFFLINE)
    val availability: StateFlow<NetworkAvailability> = _availability.asStateFlow()

    @Volatile
    private var registered = false

    fun startMonitoring(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        refresh(cm)
        if (registered) return
        registered = true
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refresh(cm)
                override fun onLost(network: Network) = refresh(cm)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    refresh(cm)
                }
            })
        }
    }

    fun refresh(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        refresh(cm)
    }

    fun isGeneralInternetAvailable(): Boolean {
        return _availability.value == NetworkAvailability.ONLINE_VALIDATED
    }

    private fun refresh(cm: ConnectivityManager) {
        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        _availability.value = when {
            caps == null -> NetworkAvailability.OFFLINE
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                NetworkAvailability.ONLINE_VALIDATED
            }
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> {
                NetworkAvailability.ZERO_MB_OR_UNVALIDATED
            }
            else -> NetworkAvailability.OFFLINE
        }
    }
}
