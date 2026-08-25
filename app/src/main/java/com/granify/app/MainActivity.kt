package com.granify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.granify.app.ui.OumatjieApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() — see docs/DECISIONS.md, "Splash screen".
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as OumatjieApplication).container
        setContent {
            OumatjieApp(container = container)
        }
    }
}

