package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.configureAppCheck
import com.example.ui.admin.MainAdminAppRoot
import com.example.ui.theme.TlBrainTestTheme

/**
 * AdminActivity launches Application 2: MAIN ADMIN APPLICATION (/admin).
 *
 * Completely independent entry point, login gate, navigation graph, and role-based
 * Firestore Security Rules authorization from MainActivity (/app).
 */
class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        com.example.core.audio.SoundManager.init(this)
        enableEdgeToEdge()
        setContent {
            TlBrainTestTheme {
                MainAdminAppRoot()
            }
        }
    }
}
