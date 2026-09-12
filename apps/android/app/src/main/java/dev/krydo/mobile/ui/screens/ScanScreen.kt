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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.camera.CameraPreview
import dev.krydo.mobile.ui.camera.rememberCameraPermissionState
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun ScanScreen(
    viewModel: AppViewModel,
    onLoaded: () -> Unit,
) {
    val state by viewModel.prove.collectAsStateWithLifecycle()
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
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Scan",
            color = KrydoColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        StatusPill(
            text = when {
                state.loading -> "Loading request…"
                cameraOn && cameraPermission.granted -> "Scanning"
                else -> "Verifier QR"
            },
            dotColor = when {
                state.loading -> KrydoColors.Warning
                cameraOn -> KrydoColors.Success
                else -> KrydoColors.Cyan
            },
        )
        Text(
            text = "Scan a verifier QR, or paste a request link / id below.",
            color = KrydoColors.TextMuted,
            fontSize = 14.sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(
                    1.dp,
                    if (cameraOn && cameraPermission.granted) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle,
                    CardShape,
                )
                .background(KrydoColors.CardSurface, CardShape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        CircularProgressIndicator(color = KrydoColors.ElectricBlue)
                        Text(
                            text = "Opening presentation request…",
                            color = KrydoColors.TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                cameraOn && cameraPermission.granted -> {
                    CameraPreview(
                        enabled = true,
                        modifier = Modifier.fillMaxSize(),
                        onQrDetected = { raw ->
                            cameraOn = false
                            viewModel.setProveInput(raw)
                            viewModel.loadRequestFromInput { onLoaded() }
                        },
                    )
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            tint = KrydoColors.BrightBlue,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = when {
                                !cameraPermission.granted -> "Camera permission needed"
                                else -> "Ready to scan"
                            },
                            color = KrydoColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = when {
                                cameraPermission.deniedPermanently ->
                                    "Enable Camera for Krydo in Android Settings → Apps."
                                !cameraPermission.granted ->
                                    "Allow camera access, then point at a verifier QR."
                                else ->
                                    "Tap Open camera and point at the QR code."
                            },
                            color = KrydoColors.TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        when {
            state.loading -> {
                Text(
                    text = "Hang tight — loading the request from Krydo API.",
                    color = KrydoColors.TextMuted,
                    fontSize = 12.sp,
                )
            }
            !cameraPermission.granted -> {
                KrydoPrimaryButton(
                    text = "Allow camera permission",
                    onClick = { cameraPermission.request() },
                )
            }
            cameraOn -> {
                KrydoSecondaryButton(text = "Close camera", onClick = { cameraOn = false })
            }
            else -> {
                KrydoPrimaryButton(
                    text = "Open camera",
                    onClick = { cameraOn = true },
                    leadingIcon = Icons.Outlined.QrCodeScanner,
                )
            }
        }

        Text(
            text = "Or paste manually",
            color = KrydoColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::setProveInput,
            label = { Text("Request URI or id") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CardShape,
            colors = fieldColors,
            enabled = !state.loading,
        )
        KrydoPrimaryButton(
            text = if (state.loading) "Loading…" else "Open request",
            onClick = { viewModel.loadRequestFromInput { onLoaded() } },
            enabled = !state.loading && state.input.isNotBlank(),
            showArrow = true,
        )
        state.error?.let {
            Text(text = it, color = KrydoColors.Error, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
