package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceConnectionStatus(
    val notificationListenerGranted: Boolean = false,
    val notificationListenerConnected: Boolean = false,
    val accessibilityServiceEnabled: Boolean = false,
    val accessibilityServiceConnected: Boolean = false
)

object ServiceStatusTracker {
    private val _status = MutableStateFlow(ServiceConnectionStatus())
    val status: StateFlow<ServiceConnectionStatus> = _status.asStateFlow()

    fun refresh(context: Context) {
        val listenerGranted = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)

        val accessibilityEnabled = isAccessibilityEnabled(context)

        _status.value = _status.value.copy(
            notificationListenerGranted = listenerGranted,
            accessibilityServiceEnabled = accessibilityEnabled
        )
    }

    fun setNotificationListenerConnected(connected: Boolean) {
        _status.value = _status.value.copy(
            notificationListenerConnected = connected,
            notificationListenerGranted = if (connected) true else _status.value.notificationListenerGranted
        )
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _status.value = _status.value.copy(
            accessibilityServiceConnected = connected,
            accessibilityServiceEnabled = if (connected) true else _status.value.accessibilityServiceEnabled
        )
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val expectedId = ComponentName(context, AtomaticAccessibilityService::class.java).flattenToString()
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledServices?.any { it.id.equals(expectedId, ignoreCase = true) } == true) {
            return true
        }
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return settingValue.split(':').any { it.equals(expectedId, ignoreCase = true) }
    }
}
