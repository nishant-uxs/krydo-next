package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.AppSettings
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.ActivityRow
import dev.krydo.mobile.ui.components.CredentialCard
import dev.krydo.mobile.ui.components.IdentityCore
import dev.krydo.mobile.ui.components.IdentityCoreMode
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.SectionHeader
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
    onOpenCredentials: () -> Unit,
    onOpenRequest: () -> Unit,
    onOpenProve: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCredential: (String) -> Unit,
    onOpenZk: () -> Unit = {},
    onOpenInbox: () -> Unit = {},
) {
    val credentials by viewModel.activeCredentials.collectAsStateWithLifecycle()
    val pinnedIds by viewModel.pinnedCredentialIds.collectAsStateWithLifecycle()
    val requests by viewModel.credentialRequests.collectAsStateWithLifecycle()
    val proofs by viewModel.zkProofs.collectAsStateWithLifecycle()
    val inbox by viewModel.issuerInbox.collectAsStateWithLifecycle()
    val authed = settings.authToken.isNotBlank() && settings.holderAddress.isNotBlank()
    val activeCount = credentials.count {
        it.status.equals("active", true) ||
            it.status.equals("issued", true) ||
            it.status.equals("verified", true)
    }
    val previewCredentials = (
        credentials.filter { it.id in pinnedIds } +
            credentials.filter { it.id !in pinnedIds }
        ).take(3)

    LaunchedEffect(authed) {
        if (authed) {
            viewModel.refreshCredentials()
            viewModel.refreshRequestFlow()
            viewModel.refreshZkProofs()
            if (settings.isIssuerOrRoot) viewModel.refreshIssuerInbox()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KrydoColors.ElectricBlue.copy(alpha = 0.14f),
                            KrydoColors.BackgroundPrimary,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KrydoWordmark()
                IconButton(onClick = onOpenSettings) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(KrydoColors.CardSurface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = KrydoColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusPill(
                    text = if (authed) "Wallet connected" else "Connect wallet to continue",
                    dotColor = if (authed) KrydoColors.Success else KrydoColors.Warning,
                )
                Spacer(modifier = Modifier.height(8.dp))
                IdentityCore(
                    coreSize = 210.dp,
                    mode = IdentityCoreMode.Idle,
                    label = if (authed) "ACTIVE" else "SETUP",
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${credentials.size} credentials · $activeCount active",
                    color = KrydoColors.TextMuted,
                    fontSize = 13.sp,
                )
                Text(
                    text = if (authed) {
                        "Holder ${settings.holderAddress.take(6)}…${settings.holderAddress.takeLast(4)}"
                    } else {
                        "Use Connect Wallet on the login screen"
                    },
                    color = KrydoColors.Cyan.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (authed && settings.did.isNotBlank()) {
                    Text(
                        text = settings.did.take(28) + "…",
                        color = KrydoColors.TextMuted,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick path tip for first-time / empty wallets
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KrydoColors.CardSurface, CardShape)
                    .clickable(onClick = if (credentials.isEmpty()) onOpenRequest else onOpenZk)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (credentials.isEmpty()) "Getting started" else "Next step",
                    color = KrydoColors.Cyan.copy(alpha = 0.95f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                )
                Text(
                    text = when {
                        credentials.isEmpty() -> "1) Request a credential → 2) Prove / Scan → 3) Share a ZK proof"
                        proofs.isEmpty() -> "You have credentials — generate a ZK proof to share privately"
                        else -> "Scan a verifier QR or open Prove to present a credential"
                    },
                    color = KrydoColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Status")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusChipHome(
                    label = "${requests.count { it.status.equals("pending", true) }} req",
                    onClick = onOpenRequest,
                    modifier = Modifier.weight(1f),
                )
                StatusChipHome(
                    label = "${proofs.size} proofs",
                    onClick = onOpenZk,
                    modifier = Modifier.weight(1f),
                )
                StatusChipHome(
                    label = if (settings.isIssuerOrRoot) {
                        "${inbox.count { it.status.equals("pending", true) }} inbox"
                    } else {
                        "${credentials.size} creds"
                    },
                    onClick = if (settings.isIssuerOrRoot) onOpenInbox else onOpenCredentials,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KrydoPrimaryButton(
                    text = "Request",
                    onClick = onOpenRequest,
                    leadingIcon = Icons.Outlined.Send,
                    modifier = Modifier.weight(1f),
                )
                KrydoSecondaryButton(
                    text = "Prove",
                    onClick = onOpenProve,
                    leadingIcon = Icons.Outlined.Shield,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            KrydoSecondaryButton(
                text = "Scan presentation QR",
                onClick = onOpenScan,
                leadingIcon = Icons.Outlined.QrCodeScanner,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader(
                title = "Stored Credentials",
                trailing = {
                    Text(
                        text = "${credentials.size} total · Manage",
                        color = KrydoColors.BrightBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onOpenCredentials),
                    )
                },
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (credentials.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "No credentials yet. Browse approved issuers and request one.",
                        color = KrydoColors.TextMuted,
                        fontSize = 13.sp,
                    )
                    KrydoPrimaryButton(
                        text = "Request credentials",
                        onClick = onOpenRequest,
                        leadingIcon = Icons.Outlined.Send,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    previewCredentials.forEach { cred ->
                        CredentialCard(
                            credential = cred,
                            pinned = cred.id in pinnedIds,
                            onClick = { onOpenCredential(cred.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionHeader(
                title = "Recent Activity",
                trailing = {
                    Text(text = "Synced", color = KrydoColors.TextMuted, fontSize = 12.sp)
                },
            )
            ActivityRow(
                title = if (authed) "Wallet session active" else "Waiting for wallet link",
                subtitle = if (authed) {
                    settings.apiBaseUrl.removePrefix("https://")
                } else {
                    "Login → Connect Wallet"
                },
                time = "—",
                iconTint = if (authed) KrydoColors.Success else KrydoColors.Warning,
            )
            val pendingReqs = requests.count { it.status.equals("pending", true) }
            if (pendingReqs > 0) {
                ActivityRow(
                    title = "$pendingReqs credential request${if (pendingReqs == 1) "" else "s"} pending",
                    subtitle = "Waiting on issuer approval",
                    time = "now",
                    iconTint = KrydoColors.Warning,
                )
            }
            if (proofs.isNotEmpty()) {
                val latest = proofs.first()
                ActivityRow(
                    title = "ZK proof ready",
                    subtitle = latest.proofType.replace('_', ' '),
                    time = "recent",
                    iconTint = KrydoColors.ElectricBlue,
                )
            }
            if (credentials.isNotEmpty()) {
                val latestCred = credentials.maxByOrNull { it.issuedAt } ?: credentials.first()
                ActivityRow(
                    title = "${latestCred.title} available",
                    subtitle = latestCred.claimType.replace('_', ' '),
                    time = latestCred.issuedAt.take(10).ifBlank { "—" },
                    iconTint = KrydoColors.Success,
                )
            }
            if (settings.isIssuerOrRoot) {
                val pendingInbox = inbox.count { it.status.equals("pending", true) }
                if (pendingInbox > 0) {
                    ActivityRow(
                        title = "$pendingInbox inbox request${if (pendingInbox == 1) "" else "s"}",
                        subtitle = "Tap Inbox to approve or reject",
                        time = "now",
                        iconTint = KrydoColors.Cyan,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChipHome(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = KrydoColors.TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(KrydoColors.CardSurface, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}
