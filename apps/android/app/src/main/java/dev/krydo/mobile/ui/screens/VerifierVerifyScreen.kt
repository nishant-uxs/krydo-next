package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.camera.CameraPreview
import dev.krydo.mobile.ui.camera.rememberCameraPermissionState
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

/**
 * Guest / verifier path — no wallet login required.
 * Paste a Krydo verify URL (or proof id) from a holder's ZK QR.
 */
@Composable
fun VerifierVerifyScreen(
    viewModel: AppViewModel,
    onBackToLogin: () -> Unit,
) {
    val ui by viewModel.verifierUi.collectAsStateWithLifecycle()
    val cameraPermission = rememberCameraPermissionState()
    var cameraOn by remember { mutableStateOf(false) }
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
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KrydoWordmark(subtitle = null)
        Text(
            text = "Verify ZK proof",
            color = KrydoColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        StatusPill(text = "No login required", dotColor = KrydoColors.Success)
        Text(
            text = "Scan or paste the holder’s proof link (/verify/…). Cryptographic check runs on Krydo — no wallet needed.",
            color = KrydoColors.TextMuted,
            fontSize = 13.sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KrydoColors.BorderBlue, CardShape)
                .padding(2.dp),
        ) {
            CameraPreview(
                enabled = cameraOn && cameraPermission.granted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                onQrDetected = { raw ->
                    cameraOn = false
                    viewModel.setVerifierInput(raw)
                    viewModel.verifyZkFromInput()
                },
            )
        }

        when {
            !cameraPermission.granted -> {
                KrydoPrimaryButton(
                    text = "Allow camera",
                    onClick = { cameraPermission.request() },
                )
            }
            cameraOn -> {
                KrydoSecondaryButton(text = "Close camera", onClick = { cameraOn = false })
                Text(
                    text = "QR auto-fills and verifies when a Krydo /verify/ link is detected.",
                    color = KrydoColors.TextMuted,
                    fontSize = 12.sp,
                )
            }
            else -> {
                KrydoSecondaryButton(text = "Open camera", onClick = { cameraOn = true })
            }
        }

        OutlinedTextField(
            value = ui.input,
            onValueChange = viewModel::setVerifierInput,
            label = { Text("Verify URL or proof ID") },
            placeholder = { Text("https://www.krydo.in/verify/…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CardShape,
            colors = fieldColors,
        )

        KrydoPrimaryButton(
            text = if (ui.loading) "Verifying…" else "Verify proof",
            onClick = viewModel::verifyZkFromInput,
            enabled = !ui.loading && ui.input.isNotBlank(),
            showArrow = true,
        )

        ui.error?.let {
            Text(text = it, color = KrydoColors.Error, fontSize = 13.sp)
        }

        ui.result?.let { result ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KrydoColors.CardSurface, CardShape)
                    .border(1.dp, KrydoColors.BorderSubtle, CardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusPill(
                    text = if (result.valid) "VALID" else "INVALID",
                    dotColor = if (result.valid) KrydoColors.Success else KrydoColors.Error,
                )
                Text(
                    text = result.reason ?: if (result.valid) "Proof checks out" else "Proof rejected",
                    color = KrydoColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                result.proof?.proofType?.let {
                    Text(text = "Type: $it", color = KrydoColors.TextMuted, fontSize = 12.sp)
                }
                result.credential?.claimType?.let {
                    Text(text = "Claim: $it", color = KrydoColors.TextMuted, fontSize = 12.sp)
                }
                result.issuer?.name?.let {
                    Text(text = "Issuer: $it", color = KrydoColors.TextMuted, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        KrydoSecondaryButton(text = "Back to login", onClick = onBackToLogin)
    }
}
