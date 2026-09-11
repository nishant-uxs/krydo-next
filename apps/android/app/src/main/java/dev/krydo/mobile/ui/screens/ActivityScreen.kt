package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.AppSettings
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.ActivityRow
import dev.krydo.mobile.ui.components.SectionHeader
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun ActivityScreen(
    viewModel: AppViewModel,
    settings: AppSettings,
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val prove by viewModel.prove.collectAsStateWithLifecycle()
    val authed = settings.authToken.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Activity",
            color = KrydoColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Recent wallet events on this device.",
            color = KrydoColors.TextMuted,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SectionHeader(title = "Today")
        ActivityRow(
            title = if (authed) "Wallet session active" else "No session",
            subtitle = settings.apiBaseUrl.removePrefix("https://"),
            time = "—",
            iconTint = if (authed) KrydoColors.Success else KrydoColors.Warning,
        )
        if (prove.verifyResult != null) {
            ActivityRow(
                title = if (prove.verifyResult!!.valid) "Presentation verified" else "Presentation failed",
                subtitle = prove.verifyResult!!.message.take(48),
                time = "now",
                iconTint = if (prove.verifyResult!!.valid) KrydoColors.Success else KrydoColors.Error,
            )
        }
        credentials.take(5).forEach { cred ->
            ActivityRow(
                title = "${cred.title} available",
                subtitle = cred.claimType,
                time = cred.issuedAt.take(10).ifBlank { "—" },
                iconTint = KrydoColors.ElectricBlue,
            )
        }
        if (credentials.isEmpty() && prove.verifyResult == null) {
            Text(
                text = "No activity yet. Sync credentials or complete a presentation.",
                color = KrydoColors.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
