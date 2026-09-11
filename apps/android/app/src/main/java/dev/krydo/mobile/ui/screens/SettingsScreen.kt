package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.settingsUi.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Paste your hosted Krydo API base URL (Render, Railway, etc). No Metro required.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = ui.urlDraft,
            onValueChange = viewModel::setUrlDraft,
            label = { Text("API base URL") },
            placeholder = { Text("https://your-app.onrender.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = viewModel::saveApiUrl, modifier = Modifier.fillMaxWidth()) {
            Text("Save URL")
        }
        OutlinedButton(
            onClick = viewModel::testConnection,
            modifier = Modifier.fillMaxWidth(),
            enabled = !ui.testing,
        ) {
            Text(if (ui.testing) "Testing…" else "Test connection (/healthz)")
        }
        if (ui.testing) CircularProgressIndicator()
        ui.testMessage?.let { msg ->
            Text(
                msg,
                color = when (ui.testOk) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onBackground
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Use mock data (offline demo)", style = MaterialTheme.typography.titleSmall)
            Text(
                if (settings.useMockData) {
                    "ON — requests/verify stay local. Turn OFF to call the pasted backend."
                } else {
                    "OFF — live backend at ${settings.apiBaseUrl}"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Switch(
                checked = settings.useMockData,
                onCheckedChange = viewModel::setUseMock,
            )
        }

        Text("Package: dev.krydo.mobile · Kotlin Compose · no Expo", style = MaterialTheme.typography.bodySmall)
    }
}
