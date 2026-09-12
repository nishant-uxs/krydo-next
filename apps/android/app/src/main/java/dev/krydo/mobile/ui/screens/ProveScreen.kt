package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.IdentityCore
import dev.krydo.mobile.ui.components.IdentityCoreMode
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun ProveScreen(
    viewModel: AppViewModel,
    onShowResult: () -> Unit,
) {
    val state by viewModel.prove.collectAsStateWithLifecycle()
    val credentials by viewModel.activeCredentials.collectAsStateWithLifecycle()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Prove",
            color = KrydoColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Paste a presentation request id or krydo://present?request=… deep link.",
            color = KrydoColors.TextMuted,
            fontSize = 14.sp,
        )

        if (state.request == null && state.presentation == null) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setProveInput,
                label = { Text("Request ID or krydo:// URI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CardShape,
                colors = fieldColors,
            )
            KrydoPrimaryButton(
                text = "Continue",
                onClick = { viewModel.loadRequestFromInput() },
                enabled = !state.loading && state.input.isNotBlank(),
                showArrow = true,
            )
        }

        if (state.loading) {
            CircularProgressIndicator(color = KrydoColors.ElectricBlue)
        }
        state.error?.let {
            Text(text = it, color = KrydoColors.Error, fontSize = 13.sp)
        }

        state.request?.let { req ->
            if (state.presentation == null) {
                StatusPill(text = "Verification request", dotColor = KrydoColors.Cyan)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KrydoColors.CardSurface, CardShape)
                        .border(1.dp, KrydoColors.BorderBlue, CardShape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Id: ${req.id}", color = KrydoColors.TextSecondary, fontSize = 13.sp)
                    Text("Reason: ${req.reason ?: "—"}", color = KrydoColors.TextSecondary, fontSize = 13.sp)
                    Text(
                        "Claim: ${req.requestedCredentials.firstOrNull()?.claimType ?: req.policy.claimType}",
                        color = KrydoColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Expires: ${req.expiresAt}", color = KrydoColors.TextMuted, fontSize = 12.sp)
                }

                Text(
                    text = "Select credential",
                    color = KrydoColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val want = req.requestedCredentials.firstOrNull()?.claimType ?: req.policy.claimType
                val matches = credentials.filter { it.claimType == want }.ifEmpty { credentials }
                if (matches.isEmpty()) {
                    Text(
                        text = "No matching credentials for $want. Refresh after signing in.",
                        color = KrydoColors.Error,
                        fontSize = 13.sp,
                    )
                }
                matches.forEach { cred ->
                    val selected = state.selectedCredentialId == cred.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) KrydoColors.ElectricBlue.copy(alpha = 0.12f) else KrydoColors.CardSurface,
                                CardShape,
                            )
                            .border(
                                1.dp,
                                if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle,
                                CardShape,
                            )
                            .clickable { viewModel.selectCredential(cred.id) }
                            .padding(14.dp),
                    ) {
                        Text(
                            text = if (selected) "✓ ${cred.title}" else cred.title,
                            color = KrydoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("${cred.claimType} · ${cred.status}", color = KrydoColors.TextMuted, fontSize = 12.sp)
                    }
                }
                KrydoPrimaryButton(
                    text = "Create presentation",
                    onClick = viewModel::createPresentation,
                    enabled = !state.loading && state.selectedCredentialId != null,
                )
                KrydoSecondaryButton(text = "Back", onClick = viewModel::clearProveFlow)
            }
        }

        state.presentation?.let {
            IdentityCore(coreSize = 160.dp, mode = IdentityCoreMode.Idle, label = "READY")
            Text(
                text = "Ready to Present",
                color = KrydoColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KrydoColors.CardSurface, CardShape)
                    .border(1.dp, KrydoColors.BorderBlue, CardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Verifiable presentation created on the Krydo server.",
                    color = KrydoColors.TextSecondary,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Next: submit it to the verifier.",
                    color = KrydoColors.TextMuted,
                    fontSize = 13.sp,
                )
            }
            KrydoPrimaryButton(
                text = "Present / Verify",
                onClick = onShowResult,
                enabled = !state.loading,
                showArrow = true,
            )
            KrydoSecondaryButton(text = "Back", onClick = viewModel::clearProveFlow)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
