package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AtomaticAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceStatusTracker.setAccessibilityConnected(true)
        ServiceStatusTracker.refresh(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Assists Atomatic by monitoring notification state transitions and verifying service readiness.
        if (event?.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            ServiceStatusTracker.setAccessibilityConnected(true)
        }
    }

    override fun onInterrupt() {
        ServiceStatusTracker.setAccessibilityConnected(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceStatusTracker.setAccessibilityConnected(false)
    }
}
