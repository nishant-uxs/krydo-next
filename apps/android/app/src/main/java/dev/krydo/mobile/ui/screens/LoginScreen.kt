package dev.krydo.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.BuildConfig
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.IdentityCore
import dev.krydo.mobile.ui.components.IdentityCoreMode
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.PillShape
import dev.krydo.mobile.wallet.FreighterConnectActivity
import dev.krydo.mobile.wallet.KnownWallet
import dev.krydo.mobile.wallet.KrydoEvmConnectActivity
import dev.krydo.mobile.wallet.WalletDetector
import dev.krydo.mobile.wallet.WalletRail
import dev.krydo.mobile.wallet.WebConnectLauncher

private enum class LoginStep {
    Onboarding,
    Wallets,
    WaitingFreighter,
    StellarRestore,
}

/**
 * Stitch onboarding → Get Started → mobile-supported wallets → session.
 * Backend: [BuildConfig.DEFAULT_API_BASE_URL] (Render).
 */
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    val ui by viewModel.settingsUi.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var step by remember { mutableStateOf(LoginStep.Onboarding) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KrydoColors.ElectricBlue,
        unfocusedBorderColor = KrydoColors.BorderSubtle,
        focusedTextColor = KrydoColors.TextPrimary,
        unfocusedTextColor = KrydoColors.TextPrimary,
        cursorColor = KrydoColors.ElectricBlue,
        focusedLabelColor = KrydoColors.BrightBlue,
        unfocusedLabelColor = KrydoColors.TextMuted,
        focusedContainerColor = KrydoColors.CardSurface,
        unfocusedContainerColor = KrydoColors.CardSurface,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KrydoColors.ElectricBlue.copy(alpha = 0.18f),
                            KrydoColors.BackgroundPrimary,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KrydoWordmark(subtitle = null)
                StatusPill(
                    text = "SELF-SOVEREIGN",
                    dotColor = KrydoColors.Cyan,
                    textColor = KrydoColors.TextSecondary,
                )
            }

            when (step) {
                LoginStep.Onboarding -> OnboardingStep(
                    onGetStarted = { step = LoginStep.Wallets },
                    onRestore = { step = LoginStep.StellarRestore },
                )
                LoginStep.Wallets -> WalletPickerStep(
                    onBack = { step = LoginStep.Onboarding },
                    onStellar = { step = LoginStep.StellarRestore },
                    onFreighterConnect = {
                        context.startActivity(Intent(context, FreighterConnectActivity::class.java))
                    },
                    onEvm = { wallet ->
                        if (wallet != null) {
                            WalletDetector.openWalletOrStore(context, wallet)
                        }
                        val intent = Intent(context, KrydoEvmConnectActivity::class.java)
                        if (wallet != null) {
                            intent.putExtra(KrydoEvmConnectActivity.EXTRA_WALLET_ID, wallet.id)
                            intent.putExtra(KrydoEvmConnectActivity.EXTRA_WALLET_PACKAGE, wallet.packageName)
                            intent.putExtra(KrydoEvmConnectActivity.EXTRA_WALLET_NAME, wallet.displayName)
                        }
                        context.startActivity(intent)
                    },
                )
                LoginStep.WaitingFreighter -> WaitingFreighterStep(
                    onRetry = {
                        context.startActivity(Intent(context, FreighterConnectActivity::class.java))
                    },
                    onManual = { step = LoginStep.StellarRestore },
                    onBack = { step = LoginStep.Wallets },
                )
                LoginStep.StellarRestore -> StellarRestoreStep(
                    uiHolder = ui.holderDraft,
                    uiToken = ui.tokenDraft,
                    onHolder = viewModel::setHolderDraft,
                    onToken = viewModel::setTokenDraft,
                    onSave = viewModel::saveStellarLoginSession,
                    onBack = { step = LoginStep.Wallets },
                    fieldColors = fieldColors,
                    message = ui.testMessage,
                    ok = ui.testOk,
                )
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    onGetStarted: () -> Unit,
    onRestore: () -> Unit,
) {
    Spacer(modifier = Modifier.height(28.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IdentityCore(coreSize = 220.dp, mode = IdentityCoreMode.Onboarding)
        Spacer(modifier = Modifier.height(12.dp))
        StatusPill(
            text = "Zero-Knowledge Encrypted",
            dotColor = KrydoColors.BrightBlue,
            textColor = KrydoColors.TextSecondary,
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = buildAnnotatedString {
            append("Your ")
            withStyle(SpanStyle(color = KrydoColors.ElectricBlue, fontWeight = FontWeight.Bold)) {
                append("identity.")
            }
        },
        color = KrydoColors.TextPrimary,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = "In your control.",
        color = KrydoColors.TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Store credentials. Prove claims. Present privately without leaking metadata.",
        color = KrydoColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    )

    Spacer(modifier = Modifier.height(36.dp))

    KrydoPrimaryButton(
        text = "Get Started",
        onClick = onGetStarted,
        showArrow = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Already have a key? ",
            color = KrydoColors.TextMuted,
            fontSize = 13.sp,
        )
        Text(
            text = "Restore wallet",
            color = KrydoColors.BrightBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onRestore),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "API · ${BuildConfig.DEFAULT_API_BASE_URL}",
        color = KrydoColors.TextMuted.copy(alpha = 0.7f),
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun WalletPickerStep(
    onBack: () -> Unit,
    onStellar: () -> Unit,
    onFreighterConnect: () -> Unit,
    onEvm: (KnownWallet?) -> Unit,
) {
    val context = LocalContext.current
    val detected = remember(context) { WalletDetector.scan(context) }
    val installed = detected.filter { it.installed }
    val notInstalled = detected.filter { !it.installed && it.wallet.packageName.isNotBlank() }

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Connect wallet",
        color = KrydoColors.TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = if (installed.isNotEmpty()) {
            "Found ${installed.size} wallet(s) on this phone. Tap one — the wallet will show a connection popup."
        } else {
            "No known wallets detected. Install MetaMask / Rainbow / Freighter, or restore a Stellar session."
        },
        color = KrydoColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    if (installed.isNotEmpty()) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "INSTALLED ON THIS PHONE",
            color = KrydoColors.Success,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        installed.forEach { item ->
            WalletOptionCard(
                title = item.wallet.displayName,
                subtitle = item.wallet.subtitle,
                badge = "INSTALLED",
                badgeColor = KrydoColors.Success,
                onClick = {
                    when (item.wallet.rail) {
                        WalletRail.EVM -> onEvm(item.wallet)
                        WalletRail.STELLAR -> {
                            if (item.wallet.id.startsWith("freighter")) {
                                onFreighterConnect()
                            } else {
                                WalletDetector.openWalletOrStore(context, item.wallet)
                                onStellar()
                            }
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "OTHER OPTIONS",
        color = KrydoColors.Cyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(10.dp))
    WalletOptionCard(
        title = "Freighter",
        subtitle = "One Approve in Freighter — no sign message",
        onClick = onFreighterConnect,
    )
    Spacer(modifier = Modifier.height(10.dp))
    WalletOptionCard(
        title = "WalletConnect (any EVM)",
        subtitle = "Open AppKit — QR / listed wallets",
        onClick = { onEvm(null) },
    )
    Spacer(modifier = Modifier.height(10.dp))
    WalletOptionCard(
        title = "Restore Stellar session",
        subtitle = "Manual fallback — paste G… + JWT",
        onClick = onStellar,
    )

    if (notInstalled.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NOT INSTALLED — TAP TO GET",
            color = KrydoColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        notInstalled.take(4).forEach { item ->
            WalletOptionCard(
                title = item.wallet.displayName,
                subtitle = "Install from Play Store",
                badge = "GET",
                badgeColor = KrydoColors.BrightBlue,
                onClick = { WalletDetector.openWalletOrStore(context, item.wallet) },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    KrydoSecondaryButton(text = "Back", onClick = onBack)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun WalletOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badge: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color = KrydoColors.Cyan,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(KrydoColors.CardSurface)
            .border(1.dp, KrydoColors.BorderBlue, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(KrydoColors.ElectricBlue.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = KrydoColors.BrightBlue,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = KrydoColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = subtitle,
                color = KrydoColors.TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun WaitingFreighterStep(
    onRetry: () -> Unit,
    onManual: () -> Unit,
    onBack: () -> Unit,
) {
    Spacer(modifier = Modifier.height(28.dp))
    Text(
        text = "Approve in Freighter",
        color = KrydoColors.TextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "1) Freighter app khul gaya — wahan unlock / ready rakho\n" +
            "2) Krydo pe wapas aake session complete karo\n" +
            "3) Website ab auto nahi khulegi",
        color = KrydoColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )
    Spacer(modifier = Modifier.height(28.dp))
    KrydoPrimaryButton(text = "Open Freighter again", onClick = onRetry, showArrow = true)
    Spacer(modifier = Modifier.height(12.dp))
    KrydoSecondaryButton(text = "Complete with G… + JWT", onClick = onManual)
    Spacer(modifier = Modifier.height(10.dp))
    KrydoSecondaryButton(text = "Back", onClick = onBack)
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun StellarRestoreStep(
    uiHolder: String,
    uiToken: String,
    onHolder: (String) -> Unit,
    onToken: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
    message: String?,
    ok: Boolean?,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Restore Stellar session",
        color = KrydoColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Manual fallback only. Prefer Freighter from Connect wallet — it returns automatically after approve + sign.",
        color = KrydoColors.TextMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = uiHolder,
        onValueChange = onHolder,
        label = { Text("Stellar address (G…)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = CardShape,
        colors = fieldColors,
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = uiToken,
        onValueChange = onToken,
        label = { Text("JWT auth token") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = CardShape,
        colors = fieldColors,
    )
    Spacer(modifier = Modifier.height(16.dp))
    KrydoPrimaryButton(text = "Save & enter", onClick = onSave, showArrow = true)
    Spacer(modifier = Modifier.height(10.dp))
    KrydoSecondaryButton(text = "Back to wallets", onClick = onBack)
    message?.let { msg ->
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = msg,
            color = if (ok == false) KrydoColors.Error else KrydoColors.Success,
            fontSize = 13.sp,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}
