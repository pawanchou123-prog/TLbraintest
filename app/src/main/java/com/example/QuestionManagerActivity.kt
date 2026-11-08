package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.configureAppCheck
import com.example.ui.questionmanager.QuestionManagerAppRoot
import com.example.ui.theme.TlBrainTestTheme

/**
 * QuestionManagerActivity launches Application 3:
 * QUESTION MANAGEMENT APPLICATION (/question-manager).
 *
 * Completely independent entry point, login gate, navigation namespace, and role-based
 * Firestore Security Rules authorization from MainActivity (/app) and AdminActivity (/admin).
 */
class QuestionManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        com.example.core.audio.SoundManager.init(this)
        enableEdgeToEdge()
        setContent {
            TlBrainTestTheme {
                QuestionManagerAppRoot()
            }
        }
    }
}
