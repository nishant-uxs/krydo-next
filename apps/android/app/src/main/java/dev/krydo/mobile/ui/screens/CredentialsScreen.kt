package dev.krydo.mobile.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.ClaimCategories
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.ClickableTxHash
import dev.krydo.mobile.ui.components.CredentialCard
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoPullRefresh
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.PillShape

@Composable
fun CredentialsScreen(
    viewModel: AppViewModel,
    onOpen: (String) -> Unit,
    onOpenRequest: () -> Unit = {},
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val archivedIds by viewModel.archivedCredentialIds.collectAsStateWithLifecycle()
    val ui by viewModel.credentialsUi.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshCredentials()
    }

    val sections = viewModel.credentialSections(credentials, archivedIds, ui.showArchived)
    val activeCount = credentials.count { it.id !in archivedIds }
    val archivedCount = credentials.count { it.id in archivedIds }

    KrydoPullRefresh(
        refreshing = ui.loading,
        onRefresh = viewModel::refreshCredentials,
    ) {
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
                    text = if (ui.showArchived) "$archivedCount archived" else "$activeCount active",
                    dotColor = KrydoColors.ElectricBlue,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip(
                    text = "Active",
                    selected = !ui.showArchived,
                    onClick = { viewModel.setCredentialsShowArchived(false) },
                )
                TabChip(
                    text = "Archived",
                    selected = ui.showArchived,
                    onClick = { viewModel.setCredentialsShowArchived(true) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            KrydoSecondaryButton(
                text = "Request from issuer",
                onClick = onOpenRequest,
            )
            if (ui.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
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
            if (!ui.loading && sections.isEmpty() && ui.error == null) {
                Text(
                    text = if (ui.showArchived) {
                        "No archived credentials. Archive from a credential’s detail screen."
                    } else {
                        "No active credentials. Request from an issuer, or check Archived."
                    },
                    color = KrydoColors.TextMuted,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { (category, list) ->
                    item(key = "hdr-$category") {
                        Text(
                            text = category.uppercase(),
                            color = KrydoColors.Cyan.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(list, key = { it.id }) { cred ->
                        CredentialCard(credential = cred, onClick = { onOpen(cred.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) KrydoColors.ElectricBlue.copy(alpha = 0.22f) else KrydoColors.CardSurface
    val border = if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle
    val fg = if (selected) KrydoColors.TextPrimary else KrydoColors.TextMuted
    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun CredentialDetailScreen(
    credential: StoredCredential?,
    archived: Boolean,
    onArchiveToggle: () -> Unit,
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
            DetailLine("Category", ClaimCategories.labelFor(credential.claimType))
            DetailLine("Claim type", credential.claimType)
            DetailLine("Claim value", credential.claimValue ?: "—")
            DetailLine("Issuer", credential.issuerName)
            DetailLine("Issuer address", credential.issuerAddress)
            DetailLine("Holder", credential.holderAddress)
            DetailLine("Status", credential.status)
            DetailLine("Summary", credential.displaySummary)
            DetailLine("Hash", credential.credentialHash)
            ClickableTxHash(txHash = credential.onChainTxHash, label = "Issue transaction", forceOnChain = true)
            Spacer(modifier = Modifier.height(4.dp))
            if (archived) {
                KrydoPrimaryButton(text = "Restore to Active", onClick = onArchiveToggle)
            } else {
                KrydoSecondaryButton(text = "Archive", onClick = onArchiveToggle)
            }
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
