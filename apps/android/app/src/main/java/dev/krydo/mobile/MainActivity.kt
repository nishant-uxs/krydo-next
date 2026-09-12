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
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.AppViewModelFactory
import dev.krydo.mobile.ui.KrydoRoot
import dev.krydo.mobile.ui.theme.KrydoTheme

sealed class KrydoDeepLink {
    data class Present(val requestId: String) : KrydoDeepLink()
    data class Auth(val address: String, val token: String) : KrydoDeepLink()
}

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<KrydoDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = parseDeepLink(intent)
        val app = application as KrydoApplication
        setContent {
            KrydoTheme {
                val vm: AppViewModel = viewModel(
                    factory = AppViewModelFactory(app.container),
                )
                KrydoRoot(
                    viewModel = vm,
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = parseDeepLink(intent)
    }

    private fun parseDeepLink(intent: Intent?): KrydoDeepLink? {
        val data: Uri = intent?.data ?: return null
        val host = data.host.orEmpty().lowercase()
        val scheme = data.scheme.orEmpty().lowercase()

        // krydo://auth?address=G…&token=…
        if (scheme == "krydo" && host == "auth") {
            val address = data.getQueryParameter("address").orEmpty().trim()
            val token = data.getQueryParameter("token").orEmpty().trim()
            if (address.startsWith("G") && token.isNotBlank()) {
                return KrydoDeepLink.Auth(address = address, token = token)
            }
            return null
        }

        // krydo://present?request=…  or https://krydo.dev/present/…
        val requestId = data.getQueryParameter("request")
            ?: data.lastPathSegment?.takeIf { it.isNotBlank() && it != "present" }
        if (!requestId.isNullOrBlank()) {
            return KrydoDeepLink.Present(requestId)
        }
        return null
    }
}
