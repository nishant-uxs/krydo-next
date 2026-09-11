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
        StatusPill(text = "Verifier QR", dotColor = KrydoColors.Cyan)
        Text(
            text = "Open the camera to scan a verifier QR. You can still paste a request link below.",
            color = KrydoColors.TextMuted,
            fontSize = 14.sp,
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
                    .height(260.dp),
            )
        }

        when {
            !cameraPermission.granted -> {
                KrydoPrimaryButton(
                    text = "Allow camera permission",
                    onClick = { cameraPermission.request() },
                )
                if (cameraPermission.deniedPermanently) {
                    Text(
                        text = "Permission denied. Enable Camera for Krydo in Android Settings → Apps.",
                        color = KrydoColors.Error,
                        fontSize = 12.sp,
                    )
                }
            }
            cameraOn -> {
                KrydoSecondaryButton(text = "Close camera", onClick = { cameraOn = false })
                Text(
                    text = "QR decode UI next — preview confirms camera works.",
                    color = KrydoColors.TextMuted,
                    fontSize = 12.sp,
                )
            }
            else -> {
                KrydoPrimaryButton(text = "Open camera", onClick = { cameraOn = true })
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::setProveInput,
            label = { Text("Request URI or id") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CardShape,
            colors = fieldColors,
        )
        KrydoPrimaryButton(
            text = "Open request",
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
