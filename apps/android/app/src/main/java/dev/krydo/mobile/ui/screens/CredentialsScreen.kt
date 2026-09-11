package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.CredentialCard
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun CredentialsScreen(
    viewModel: AppViewModel,
    onOpen: (String) -> Unit,
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val ui by viewModel.credentialsUi.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshCredentials()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                KrydoWordmark(subtitle = null)
                Text(
                    text = "Your Credentials",
                    color = KrydoColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            StatusPill(
                text = "${credentials.size} stored",
                dotColor = KrydoColors.ElectricBlue,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loaded from your hosted Krydo API for the signed-in holder.",
            color = KrydoColors.TextMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        KrydoPrimaryButton(
            text = if (ui.loading) "Refreshing…" else "Refresh",
            onClick = viewModel::refreshCredentials,
            enabled = !ui.loading,
        )
        if (ui.loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KrydoColors.ElectricBlue)
            }
        }
        ui.error?.let {
            Text(
                text = it,
                color = KrydoColors.Error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (!ui.loading && credentials.isEmpty() && ui.error == null) {
            Text(
                text = "No credentials yet. Issue one to this holder address, then refresh.",
                color = KrydoColors.TextMuted,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(credentials, key = { it.id }) { cred ->
                CredentialCard(credential = cred, onClick = { onOpen(cred.id) })
            }
        }
    }
}

@Composable
fun CredentialDetailScreen(
    credential: StoredCredential?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = credential?.title ?: "Not found",
            color = KrydoColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        if (credential != null) {
            DetailLine("Claim type", credential.claimType)
            DetailLine("Issuer", credential.issuerAddress)
            DetailLine("Holder", credential.holderAddress)
            DetailLine("Status", credential.status)
            DetailLine("Summary", credential.displaySummary)
            DetailLine("Hash", "${credential.credentialHash.take(16)}…")
        }
        Spacer(modifier = Modifier.height(8.dp))
        KrydoSecondaryButton(text = "Back", onClick = onBack)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label.uppercase(), color = KrydoColors.TextMuted, fontSize = 11.sp, letterSpacing = 0.8.sp)
        Text(text = value, color = KrydoColors.TextSecondary, fontSize = 14.sp)
    }
}
