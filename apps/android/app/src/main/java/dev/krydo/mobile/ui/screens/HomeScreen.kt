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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
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
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
    onOpenCredentials: () -> Unit,
    onOpenProve: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCredential: (String) -> Unit,
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val authed = settings.authToken.isNotBlank() && settings.holderAddress.isNotBlank()
    val activeCount = credentials.count {
        it.status.equals("active", true) ||
            it.status.equals("issued", true) ||
            it.status.equals("verified", true)
    }

    LaunchedEffect(authed) {
        if (authed && credentials.isEmpty()) {
            viewModel.refreshCredentials()
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
                            imageVector = Icons.Outlined.Notifications,
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
                    text = if (authed) "Identity core ready" else "Connect wallet in Settings",
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
                        "Paste SIWS JWT in Settings to sync"
                    },
                    color = KrydoColors.Cyan.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KrydoPrimaryButton(
                    text = "Prove",
                    onClick = onOpenProve,
                    leadingIcon = Icons.Outlined.Shield,
                    modifier = Modifier.weight(1f),
                )
                KrydoSecondaryButton(
                    text = "Scan",
                    onClick = onOpenScan,
                    leadingIcon = Icons.Outlined.QrCodeScanner,
                    modifier = Modifier.weight(1f),
                )
            }

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
                Text(
                    text = "No credentials yet. Issue one to this holder, then refresh.",
                    color = KrydoColors.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    credentials.take(3).forEach { cred ->
                        CredentialCard(
                            credential = cred,
                            onClick = { onOpenCredential(cred.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionHeader(
                title = "Recent Activity",
                trailing = {
                    Text(text = "Today", color = KrydoColors.TextMuted, fontSize = 12.sp)
                },
            )
            ActivityRow(
                title = if (authed) "Identity core synchronized" else "Waiting for wallet link",
                subtitle = if (authed) {
                    settings.apiBaseUrl.removePrefix("https://")
                } else {
                    "Settings → paste JWT"
                },
                time = "—",
                iconTint = if (authed) KrydoColors.Success else KrydoColors.Warning,
            )
            if (credentials.isNotEmpty()) {
                ActivityRow(
                    title = "${credentials.first().title} ready",
                    subtitle = credentials.first().claimType,
                    time = "now",
                    iconTint = KrydoColors.ElectricBlue,
                )
            }
        }
    }
}
