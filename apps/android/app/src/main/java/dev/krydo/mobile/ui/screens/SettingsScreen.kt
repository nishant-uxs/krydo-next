package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoTopBar
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit = {},
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.settingsUi.collectAsStateWithLifecycle()
    var showToken by remember { mutableStateOf(false) }
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
            .padding(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KrydoTopBar(
            title = "Settings",
            subtitle = "API, session, and diagnostics",
            onBack = onBack,
        )
        Text(
            text = "Login is on the Connect Wallet gate. Advanced: paste a SIWS JWT only if needed.",
            color = KrydoColors.TextMuted,
            fontSize = 14.sp,
        )

        OutlinedTextField(
            value = ui.urlDraft,
            onValueChange = viewModel::setUrlDraft,
            label = { Text("API base URL") },
            placeholder = { Text("https://krydo.onrender.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CardShape,
            colors = fieldColors,
        )
        OutlinedTextField(
            value = ui.holderDraft,
            onValueChange = viewModel::setHolderDraft,
            label = { Text("Holder Stellar address (G…)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CardShape,
            colors = fieldColors,
        )
        OutlinedTextField(
            value = ui.tokenDraft,
            onValueChange = viewModel::setTokenDraft,
            label = { Text("JWT auth token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CardShape,
            colors = fieldColors,
            visualTransformation = if (showToken) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        imageVector = if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showToken) "Hide token" else "Show token",
                        tint = KrydoColors.TextMuted,
                    )
                }
            },
        )
        KrydoPrimaryButton(text = "Save", onClick = viewModel::saveSettings)
        KrydoSecondaryButton(
            text = "Sign out (clear wallet session)",
            onClick = viewModel::clearLoginSession,
        )
        KrydoSecondaryButton(
            text = if (ui.testing) "Testing…" else "Test connection (/healthz)",
            onClick = viewModel::testConnection,
            enabled = !ui.testing,
        )
        if (ui.testing) CircularProgressIndicator(color = KrydoColors.ElectricBlue)
        ui.testMessage?.let { msg ->
            Text(
                text = msg,
                color = when (ui.testOk) {
                    true -> KrydoColors.Success
                    false -> KrydoColors.Error
                    null -> KrydoColors.TextSecondary
                },
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Saved holder: ${settings.holderAddress.ifBlank { "—" }}",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
        Text(
            text = "Role: ${settings.walletRole}",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
        Text(
            text = "DID: ${settings.did.ifBlank { "—" }}",
            color = KrydoColors.Cyan.copy(alpha = 0.9f),
            fontSize = 12.sp,
        )
        Text(
            text = "ZK proving runs on Krydo API today (device-side proving is on the roadmap).",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
        Text(
            text = "Token set: ${if (settings.authToken.isBlank()) "no" else "yes"}",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
        Text(
            text = "Package: dev.krydo.mobile · Stellar Blue UI",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
    }
}
