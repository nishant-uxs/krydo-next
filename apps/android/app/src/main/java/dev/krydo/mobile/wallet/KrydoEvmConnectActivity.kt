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
import com.reown.appkit.client.AppKit
import com.reown.appkit.ui.AppKitSheet
import dev.krydo.mobile.KrydoApplication
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.KrydoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * EVM connect + SIWE host.
 * Requires REOWN_PROJECT_ID and an active AppKit session (wallet approved via WalletConnect).
 */
class KrydoEvmConnectActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EvmAppKitBridge.ready) {
            Toast.makeText(
                this,
                "Set reown.projectId in local.properties, rebuild, then retry.",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }

        AppKit.register(this)

        setContent {
            KrydoTheme {
                var status by remember { mutableStateOf("Connect a wallet, then complete SIWE.") }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KrydoColors.BackgroundPrimary)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Connect EVM Wallet",
                        color = KrydoColors.TextPrimary,
                        fontSize = 24.sp,
                    )
                    Text(
                        text = "1) Open AppKit and approve Krydo in MetaMask / Rainbow / …\n" +
                            "2) Tap Complete SIWE to authenticate with the Krydo API.",
                        color = KrydoColors.TextMuted,
                        fontSize = 14.sp,
                    )
                    Text(text = status, color = KrydoColors.Cyan, fontSize = 13.sp)
                    KrydoPrimaryButton(
                        text = "Open AppKit",
                        onClick = {
                            runCatching {
                                AppKitSheet().show(supportFragmentManager, "AppKit")
                            }.onFailure {
                                status = it.message ?: "Could not open AppKit"
                            }
                        },
                    )
                    KrydoPrimaryButton(
                        text = "Refresh session status",
                        onClick = {
                            val account = runCatching { AppKit.getAccount() }.getOrNull()
                            status = if (account == null) {
                                "No AppKit account yet — approve Krydo in your wallet."
                            } else {
                                "Connected ${account.address} on ${account.chain.id}"
                            }
                        },
                    )
                    KrydoPrimaryButton(
                        text = "Complete SIWE",
                        onClick = {
                            status = "Running SIWE…"
                            completeSiwe { status = it }
                        },
                    )
                    KrydoSecondaryButton(text = "Cancel", onClick = { finish() })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { AppKit.unregister() }
    }

    private fun completeSiwe(onStatus: (String) -> Unit) {
        val account = runCatching { AppKit.getAccount() }.getOrNull()
        val address = account?.address
        val chainRef = account?.chain?.chainReference?.toIntOrNull()
            ?: account?.chain?.id?.substringAfterLast(":")?.toIntOrNull()
        if (address.isNullOrBlank() || chainRef == null) {
            onStatus("Connect a wallet session first")
            Toast.makeText(this, "No EVM account in AppKit session", Toast.LENGTH_SHORT).show()
            return
        }
        if (!WalletChains.isSupportedEvm(chainRef)) {
            onStatus("Unsupported chain $chainRef")
            return
        }

        val app = application as KrydoApplication
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    SiweClient(app.container.settingsRepository).authenticate(
                        address = address,
                        chainId = chainRef,
                        signMessage = { message ->
                            SiweClient.signWithAppKit(message, address)
                        },
                    )
                }
                app.container.settingsRepository.setAuthToken(result.token)
                app.container.settingsRepository.setHolderAddress(address)
                app.container.settingsRepository.setOnboardingDone(true)
                app.container.walletSessionStore.upsert(
                    WalletAccount(
                        chainType = "EVM",
                        chainId = WalletChains.evmCaip2(chainRef),
                        address = address.lowercase(),
                        walletProvider = "reown-appkit",
                        label = "EVM Wallet",
                    ),
                )
                onStatus("EVM session saved")
                Toast.makeText(this@KrydoEvmConnectActivity, "EVM session saved", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (t: Throwable) {
                onStatus(t.message ?: "SIWE failed")
                Toast.makeText(this@KrydoEvmConnectActivity, t.message ?: "SIWE failed", Toast.LENGTH_LONG).show()
            }
        }
    }
}
