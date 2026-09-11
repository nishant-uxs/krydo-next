package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.clickable
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
fun ProveScreen(
    viewModel: AppViewModel,
    onShowResult: () -> Unit,
) {
    val state by viewModel.prove.collectAsStateWithLifecycle()
    val credentials = viewModel.credentials

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Prove", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Open a presentation request from a verifier, then select a credential and create a presentation.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.request == null && state.presentation == null) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setProveInput,
                label = { Text("Request ID or krydo:// URI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = { viewModel.loadRequestFromInput() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
            ) { Text("Continue") }
            OutlinedButton(
                onClick = viewModel::useDemoRequest,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Try demo request") }
            Text(
                "Current proofs use DEMO / MOCK PROOF — not cryptographically valid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        state.request?.let { req ->
            if (state.presentation == null) {
                Text("Verification request", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Id: ${req.id}")
                        Text("Reason: ${req.reason ?: "—"}")
                        Text("Claim: ${req.requestedCredentials.firstOrNull()?.claimType ?: req.policy.claimType}")
                        Text("Expires: ${req.expiresAt}")
                    }
                }
                Text("Select credential", style = MaterialTheme.typography.titleSmall)
                credentials
                    .filter {
                        val want = req.requestedCredentials.firstOrNull()?.claimType ?: req.policy.claimType
                        it.claimType == want
                    }
                    .ifEmpty { credentials }
                    .forEach { cred ->
                        val selected = state.selectedCredentialId == cred.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectCredential(cred.id) },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    if (selected) "✓ ${cred.title}" else cred.title,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text("${cred.issuerName} · ${cred.status}")
                            }
                        }
                    }
                Button(
                    onClick = viewModel::createPresentation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading && state.selectedCredentialId != null,
                ) { Text("Create DEMO presentation") }
                OutlinedButton(onClick = viewModel::clearProveFlow, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }

        state.presentation?.let {
            Text("Presentation ready", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.presentationLabel ?: "DEMO / MOCK PROOF", style = MaterialTheme.typography.titleSmall)
                    Text("This presentation was produced with a mock prover and is not cryptographically valid.")
                }
            }
            Button(
                onClick = {
                    viewModel.verifyPresentation()
                    onShowResult()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
            ) { Text("Present / Verify") }
            OutlinedButton(onClick = viewModel::clearProveFlow, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
