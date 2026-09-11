package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel

@Composable
fun ScanScreen(
    viewModel: AppViewModel,
    onLoaded: () -> Unit,
) {
    val state by viewModel.prove.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Scan", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Camera QR comes later. Paste a krydo://present?request=… link or request id.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::setProveInput,
            label = { Text("Request URI or id") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = { viewModel.loadRequestFromInput { onLoaded() } },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading,
        ) { Text("Open request") }
        OutlinedButton(
            onClick = {
                viewModel.useDemoRequest()
                viewModel.loadRequestFromInput { onLoaded() }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Try demo request") }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
