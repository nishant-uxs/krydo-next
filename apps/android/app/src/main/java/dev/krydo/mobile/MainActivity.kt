package dev.krydo.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.krydo.mobile.ui.KrydoRoot
import dev.krydo.mobile.ui.theme.KrydoTheme
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = extractRequestId(intent)
        val app = application as KrydoApplication
        setContent {
            KrydoTheme {
                val vm: AppViewModel = viewModel(
                    factory = AppViewModelFactory(app.container),
                )
                KrydoRoot(
                    viewModel = vm,
                    pendingRequestId = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = extractRequestId(intent)
    }

    private fun extractRequestId(intent: Intent?): String? {
        val data: Uri = intent?.data ?: return null
        return data.getQueryParameter("request")
            ?: data.lastPathSegment?.takeIf { it.isNotBlank() && it != "present" }
    }
}
