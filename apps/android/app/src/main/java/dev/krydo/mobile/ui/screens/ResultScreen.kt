package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel

@Composable
fun ResultScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.prove.collectAsStateWithLifecycle()

    LaunchedEffect(state.presentation) {
        if (state.presentation != null && state.verifyResult == null && !state.loading) {
            viewModel.verifyPresentation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Result", style = MaterialTheme.typography.headlineMedium)
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.verifyResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (result.valid) "Valid presentation" else "Invalid presentation",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (result.valid) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                    Text(result.message)
                    result.checks?.forEach { (k, v) ->
                        Text("$k: ${if (v) "pass" else "fail"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        state.presentationLabel?.let {
            Text("Proof label: $it", style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
