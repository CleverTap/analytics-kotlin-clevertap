package com.clevertap.segment.kotlin.testapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.clevertap.segment.kotlin.testapp.ui.home.HomeScreen
import com.clevertap.segment.kotlin.testapp.ui.home.HomeViewModel
import com.clevertap.segment.kotlin.testapp.ui.home.MainViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = MainViewModelFactory(
            analytics = CleverTapSegmentApplication.analytics,
            getCleverTap = { CleverTapSegmentApplication.ct }
        )

        val vm: HomeViewModel by viewModels { factory }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                color = MaterialTheme.colorScheme.background
            ) {
                HomeScreen(viewModel = vm)
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            CleverTapSegmentApplication.ct?.pushNotificationClickedEvent(intent.extras)
        }
        if (Intent.ACTION_VIEW == intent.action) {
            val data = intent.data
            if (data != null) {
                Log.d("INTENT_URI", data.toString())
                handleDeepLink(data)
            }
        }
    }

    // Handle deep links
    private fun handleDeepLink(data: Uri) {
        val scheme = data.scheme
        Log.d("DEEP_LINK", scheme ?: "")
    }
}
