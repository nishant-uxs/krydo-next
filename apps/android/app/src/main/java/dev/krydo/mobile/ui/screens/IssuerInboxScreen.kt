package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.IssuerRequestRepository
import dev.krydo.mobile.network.CredentialRequestDto
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoPullRefresh
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun IssuerInboxScreen(viewModel: AppViewModel) {
    val inbox by viewModel.issuerInbox.collectAsStateWithLifecycle()
    val ui by viewModel.issuerUi.collectAsStateWithLifecycle()
    val pending = inbox.filter { it.status.equals("pending", ignoreCase = true) }

    LaunchedEffect(Unit) { viewModel.refreshIssuerInbox() }

    KrydoPullRefresh(
        refreshing = ui.loading,
        onRefresh = viewModel::refreshIssuerInbox,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KrydoColors.BackgroundPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KrydoWordmark(subtitle = null)
            Text(
                text = "Issuer inbox",
                color = KrydoColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Approve pending holder requests and issue claim values. On-chain wallet signing is required unless the API enables demo off-chain issue.",
                color = KrydoColors.TextMuted,
                fontSize = 13.sp,
            )
            StatusPill(
                text = "${pending.size} pending · ${inbox.size} total",
                dotColor = KrydoColors.ElectricBlue,
            )
            ui.error?.let { Text(it, color = KrydoColors.Error, fontSize = 13.sp) }
            ui.successMessage?.let { Text(it, color = KrydoColors.Success, fontSize = 13.sp) }

            if (pending.isEmpty()) {
                Text(
                    text = "No pending requests for this issuer wallet.",
                    color = KrydoColors.TextMuted,
                    fontSize = 13.sp,
                )
            }

            pending.forEach { req ->
                IssuerRequestCard(
                    request = req,
                    acting = ui.acting,
                    onReject = { viewModel.rejectIssuerRequest(req.id, "Rejected from mobile") },
                    onApprove = { summary, value ->
                        viewModel.approveIssuerRequest(req.id, summary, value, "Issued from Krydo Android")
                    },
                )
            }
        }
    }
}

@Composable
private fun IssuerRequestCard(
    request: CredentialRequestDto,
    acting: Boolean,
    onReject: () -> Unit,
    onApprove: (summary: String, value: String) -> Unit,
) {
    var summary by remember(request.id) {
        mutableStateOf(IssuerRequestRepository.labelForClaim(request.claimType))
    }
    var value by remember(request.id) { mutableStateOf("") }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KrydoColors.ElectricBlue,
        unfocusedBorderColor = KrydoColors.BorderSubtle,
        focusedTextColor = KrydoColors.TextPrimary,
        unfocusedTextColor = KrydoColors.TextPrimary,
        cursorColor = KrydoColors.ElectricBlue,
        focusedLabelColor = KrydoColors.TextSecondary,
        unfocusedLabelColor = KrydoColors.TextMuted,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KrydoColors.CardSurface, CardShape)
            .border(1.dp, KrydoColors.BorderSubtle, CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = IssuerRequestRepository.labelForClaim(request.claimType),
            color = KrydoColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
        Text(
            text = "From ${request.requesterAddress.take(6)}…${request.requesterAddress.takeLast(4)}",
            color = KrydoColors.Cyan.copy(alpha = 0.9f),
            fontSize = 12.sp,
        )
        request.message?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, color = KrydoColors.TextMuted, fontSize = 12.sp)
        }
        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Claim summary") },
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            singleLine = true,
        )
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Claim value") },
            placeholder = { Text("e.g. 780") },
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KrydoSecondaryButton(
                text = "Reject",
                onClick = onReject,
                enabled = !acting,
                modifier = Modifier.weight(1f),
            )
            KrydoPrimaryButton(
                text = if (acting) "…" else "Issue",
                onClick = {
                    if (summary.isNotBlank() && value.isNotBlank()) {
                        onApprove(summary, value)
                    }
                },
                enabled = !acting && summary.isNotBlank() && value.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
