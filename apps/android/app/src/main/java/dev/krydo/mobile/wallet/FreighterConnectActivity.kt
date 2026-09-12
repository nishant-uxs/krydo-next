package dev.krydo.mobile.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.krydo.mobile.BuildConfig
import dev.krydo.mobile.KrydoApplication
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.KrydoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Freighter one-tap connect:
 * Approve Krydo in Freighter → session saved (no second sign-in message).
 */
class FreighterConnectActivity : FragmentActivity() {
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!WcCoreBridge.signReady) {
            Toast.makeText(
                this,
                "WalletConnect not ready. Restart Krydo and try again.",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }

        val statusState = mutableStateOf("Opening Freighter — Approve Krydo once…")
        val busyState = mutableStateOf(true)

        setContent {
            KrydoTheme {
                var status by remember { statusState }
                var busy by remember { busyState }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KrydoColors.BackgroundPrimary)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Connect Freighter",
                        color = KrydoColors.TextPrimary,
                        fontSize = 24.sp,
                    )
                    Text(
                        text = "Freighter mein sirf ek baar Approve dabao — sign-in message nahi aayega.",
                        color = KrydoColors.TextMuted,
                        fontSize = 14.sp,
                    )
                    Text(text = status, color = KrydoColors.Cyan, fontSize = 13.sp)

                    KrydoPrimaryButton(
                        text = if (busy) "Waiting on Freighter…" else "Try again",
                        onClick = {
                            if (busy) return@KrydoPrimaryButton
                            busy = true
                            status = "Opening Freighter…"
                            runConnect(statusState, busyState)
                        },
                        enabled = !busy,
                    )
                    KrydoSecondaryButton(text = "Cancel", onClick = { finish() })
                }
            }
        }

        if (!started) {
            started = true
            window.decorView.post { runConnect(statusState, busyState) }
        }
    }

    private fun runConnect(
        statusState: androidx.compose.runtime.MutableState<String>,
        busyState: androidx.compose.runtime.MutableState<Boolean>,
    ) {
        val app = application as KrydoApplication
        lifecycleScope.launch {
            try {
                busyState.value = true
                statusState.value = "Opening Freighter — Approve once…"
                // Always use the live Render API for Freighter login (avoid stale/wrong Datastore URL → HTML).
                app.container.settingsRepository.setApiBaseUrl(BuildConfig.DEFAULT_API_BASE_URL)
                val session = FreighterWcClient.connectFreighter(this@FreighterConnectActivity)
                statusState.value = "Approved ${session.address.take(6)}… — sign in to prove wallet"
                val result = withContext(Dispatchers.IO) {
                    SiwsClient(app.container.settingsRepository).authenticateFromWalletConnect(
                        session = session,
                        signMessage = { message ->
                            withContext(Dispatchers.Main) {
                                statusState.value = "Confirm Sign Message in Freighter…"
                            }
                            FreighterWcClient.signMessage(session, message)
                        },
                    )
                }

                app.container.settingsRepository.setAuthToken(result.token)
                app.container.settingsRepository.setHolderAddress(result.address)
                val role = result.role
                    ?: dev.krydo.mobile.util.JwtPeek.role(result.token)
                    ?: "user"
                app.container.settingsRepository.setWalletRole(role)
                app.container.settingsRepository.setOnboardingDone(true)
                app.container.walletSessionStore.upsert(
                    WalletAccount(
                        chainType = "STELLAR",
                        chainId = WalletChains.STELLAR_TESTNET,
                        address = result.address,
                        walletProvider = "freighter-wc",
                        label = "Freighter",
                    ),
                )
                statusState.value = "Connected"
                busyState.value = false
                Toast.makeText(this@FreighterConnectActivity, "Freighter connected", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (t: Throwable) {
                statusState.value = t.message ?: "Freighter connect failed"
                busyState.value = false
                Toast.makeText(this@FreighterConnectActivity, statusState.value, Toast.LENGTH_LONG).show()
            }
        }
    }
}
