package com.example.ui.student

import androidx.compose.runtime.Composable
import com.example.ui.PortalViewModel

/**
 * Legacy bridge preserved so existing multi-portal files compile cleanly while
 * Application 1 (/app) runs independently via StudentAppRoot().
 */
@Composable
fun StudentPortalScreen(
    @Suppress("UNUSED_PARAMETER") viewModel: PortalViewModel,
    @Suppress("UNUSED_PARAMETER") onSignOut: () -> Unit
) {
    StudentAppRoot()
}
