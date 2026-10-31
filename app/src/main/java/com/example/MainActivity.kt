package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.security.HaniXSecurityShield
import com.example.navigation.HaniXUrduApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HaniXViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HaniXViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Global Crash Handler for diagnostics
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("HaniXCrash", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Allow video screen recording while blocking all internal audio capture/copying (ALLOW_CAPTURE_BY_NONE)
        HaniXSecurityShield.enforceScreenRecordAllowedButAudioBlocked(this)
        HaniXSecurityShield.getOrCreatePrivateOfflineVaultDir(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HaniXUrduApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HaniXSecurityShield.enforceScreenRecordAllowedButAudioBlocked(this)
    }
}
