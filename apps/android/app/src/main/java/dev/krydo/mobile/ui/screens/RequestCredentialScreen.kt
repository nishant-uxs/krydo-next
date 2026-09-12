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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.IssuerRequestRepository
import dev.krydo.mobile.network.CredentialRequestDto
import dev.krydo.mobile.network.IssuerDto
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.ClickableTxHash
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.SectionHeader
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun RequestCredentialScreen(
    viewModel: AppViewModel,
) {
    val issuers by viewModel.issuers.collectAsStateWithLifecycle()
    val requests by viewModel.credentialRequests.collectAsStateWithLifecycle()
    val ui by viewModel.requestUi.collectAsStateWithLifecycle()

    var selectedIssuer by remember { mutableStateOf<IssuerDto?>(null) }
    var selectedClaim by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshRequestFlow()
    }

    LaunchedEffect(selectedIssuer) {
        val types = IssuerRequestRepository.claimTypesForCategory(selectedIssuer?.category)
        if (selectedClaim == null || selectedClaim !in types) {
            selectedClaim = types.firstOrNull()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            KrydoWordmark(subtitle = null)
            Text(
                text = "Request Credentials",
                color = KrydoColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick an approved issuer, choose a claim, and send a request. When they issue, it lands in Credentials.",
                color = KrydoColors.TextMuted,
                fontSize = 13.sp,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = "${issuers.size} issuers available",
                    dotColor = KrydoColors.ElectricBlue,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (ui.loading) "Refreshing…" else "Refresh",
                    color = KrydoColors.BrightBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !ui.loading) {
                        viewModel.refreshRequestFlow()
                    },
                )
            }
        }

        if (ui.loading && issuers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KrydoColors.ElectricBlue)
                }
            }
        }

        ui.error?.let { err ->
            item {
                Text(text = err, color = KrydoColors.Error, fontSize = 13.sp)
            }
        }

        ui.successMessage?.let { ok ->
            item {
                Text(text = ok, color = KrydoColors.Success, fontSize = 13.sp)
            }
        }

        item { SectionHeader(title = "Available issuers") }

        if (!ui.loading && issuers.isEmpty()) {
            item {
                Text(
                    text = "No active issuers yet. Root Authority must approve an issuer wallet on the web app (/issuers).",
                    color = KrydoColors.TextMuted,
                    fontSize = 13.sp,
                )
            }
        }

        items(issuers, key = { it.id }) { issuer ->
            IssuerPickCard(
                issuer = issuer,
                selected = selectedIssuer?.id == issuer.id,
                onClick = { selectedIssuer = issuer },
            )
        }

        if (selectedIssuer != null) {
            item {
                SectionHeader(title = "Claim type")
                Spacer(modifier = Modifier.height(8.dp))
                val types = IssuerRequestRepository.claimTypesForCategory(selectedIssuer?.category)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { claim ->
                        ClaimChip(
                            label = IssuerRequestRepository.labelForClaim(claim),
                            selected = selectedClaim == claim,
                            onClick = { selectedClaim = claim },
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Optional message") },
                    placeholder = { Text("e.g. Need score range for loan demo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KrydoColors.ElectricBlue,
                        unfocusedBorderColor = KrydoColors.BorderSubtle,
                        focusedTextColor = KrydoColors.TextPrimary,
                        unfocusedTextColor = KrydoColors.TextPrimary,
                        focusedLabelColor = KrydoColors.TextSecondary,
                        unfocusedLabelColor = KrydoColors.TextMuted,
                        cursorColor = KrydoColors.ElectricBlue,
                    ),
                    maxLines = 3,
                )
            }

            item {
                KrydoPrimaryButton(
                    text = if (ui.submitting) "Sending…" else "Request from ${selectedIssuer!!.name}",
                    onClick = {
                        val claim = selectedClaim ?: return@KrydoPrimaryButton
                        viewModel.submitCredentialRequest(
                            claimType = claim,
                            issuer = selectedIssuer,
                            message = message,
                        )
                    },
                    enabled = !ui.submitting && selectedClaim != null,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "My requests")
        }

        if (requests.isEmpty()) {
            item {
                Text(
                    text = "No requests yet. Choose an issuer above to start.",
                    color = KrydoColors.TextMuted,
                    fontSize = 13.sp,
                )
            }
        } else {
            items(requests, key = { it.id }) { req ->
                RequestStatusCard(
                    request = req,
                    issuerName = issuers.find {
                        it.walletAddress.equals(req.issuerAddress.orEmpty(), ignoreCase = true)
                    }?.name,
                    onCancel = if (req.status.equals("pending", ignoreCase = true)) {
                        { viewModel.cancelCredentialRequest(req.id) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun IssuerPickCard(
    issuer: IssuerDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (selected) KrydoColors.SurfaceElevated else KrydoColors.CardSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle,
                shape = CardShape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(KrydoColors.SurfaceElevated, CircleShape)
                .border(1.dp, KrydoColors.BorderBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Apartment,
                contentDescription = null,
                tint = KrydoColors.BrightBlue,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issuer.name,
                color = KrydoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = issuer.category.replace('_', ' '),
                color = KrydoColors.TextMuted,
                fontSize = 12.sp,
            )
            Text(
                text = "${issuer.walletAddress.take(6)}…${issuer.walletAddress.takeLast(4)}",
                color = KrydoColors.Cyan.copy(alpha = 0.85f),
                fontSize = 11.sp,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = KrydoColors.Success,
            )
        }
    }
}

@Composable
private fun ClaimChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) KrydoColors.SurfaceElevated else KrydoColors.CardSurface)
            .border(
                1.dp,
                if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = KrydoColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RequestStatusCard(
    request: CredentialRequestDto,
    issuerName: String?,
    onCancel: (() -> Unit)?,
) {
    val statusColor = when (request.status.lowercase()) {
        "pending" -> KrydoColors.Warning
        "issued", "approved" -> KrydoColors.Success
        "rejected", "denied" -> KrydoColors.Error
        else -> KrydoColors.TextMuted
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrydoColors.CardSurface)
            .border(1.dp, KrydoColors.BorderSubtle, CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = IssuerRequestRepository.labelForClaim(request.claimType),
                color = KrydoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            StatusPill(text = request.status, dotColor = statusColor)
        }
        Text(
            text = issuerName ?: request.issuerAddress?.let {
                "${it.take(6)}…${it.takeLast(4)}"
            } ?: "Open request",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
        request.responseMessage?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, color = KrydoColors.TextSecondary, fontSize = 12.sp)
        }
        ClickableTxHash(txHash = request.onChainTxHash, label = "Request transaction", forceOnChain = true)
        if (onCancel != null) {
            KrydoSecondaryButton(text = "Cancel request", onClick = onCancel)
        }
    }
}
