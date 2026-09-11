package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.krydo.mobile.data.AppSettings
import dev.krydo.mobile.ui.components.KrydoBrandHeader

@Composable
fun HomeScreen(
    settings: AppSettings,
    onOpenCredentials: () -> Unit,
    onOpenProve: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KrydoBrandHeader(
            subtitle = "Privacy-preserving credential presentations",
            logoSize = 72.dp,
        )
        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (settings.useMockData) "Mode: MOCK / offline demo" else "Mode: LIVE API",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(text = settings.apiBaseUrl, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = onOpenProve, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Open Prove")
        }
        OutlinedButton(onClick = onOpenCredentials, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Browse credentials")
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Backend Settings")
        }
    }
}
