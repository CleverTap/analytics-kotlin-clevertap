package com.clevertap.segment.kotlin.testapp

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.segment.analytics.kotlin.android.Analytics
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.destinations.CleverTapDestination

class CleverTapSegmentApplication : Application() {

    companion object {
        private const val WRITE_KEY = "<YOUR WRITE KEY>"
        private const val TAG = "CTSegmentApplication"
        var analytics: Analytics? = null
        var ct : CleverTapAPI? = null
    }

    override fun onCreate() {
        CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.VERBOSE)
        Analytics.debugLogsEnabled = true
        ActivityLifecycleCallback.register(this)
        super.onCreate()

        // Initialize Segment Analytics with CleverTap destination
        analytics = Analytics(WRITE_KEY, applicationContext).apply {
            this.add(plugin = CleverTapDestination(applicationContext, ::cleverTapIntegrationReady))
        }
    }

    private fun cleverTapIntegrationReady(cleverTapInstance: CleverTapAPI) {
        Log.i(TAG, "CleverTap integration ready")
        ct = cleverTapInstance
        ct?.initializeInbox()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CleverTapAPI.createNotificationChannel(
                applicationContext, 
                "BRTesting", 
                "YourChannelName",
                "YourChannelDescription",
                NotificationManager.IMPORTANCE_HIGH,
                true
            )
        }
    }
}
