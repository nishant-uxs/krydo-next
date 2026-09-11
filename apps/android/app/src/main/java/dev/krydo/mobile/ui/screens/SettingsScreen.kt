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
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.settingsUi.collectAsStateWithLifecycle()
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
            text = "Settings",
            color = KrydoColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Advanced session controls. Login is on the Connect Wallet gate. Stellar: paste a SIWS JWT from the web app until native SIWS ships. EVM: use Connect EVM on login (Reown AppKit).",
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
            text = "Package: dev.krydo.mobile · Stellar Blue UI",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
    }
}
