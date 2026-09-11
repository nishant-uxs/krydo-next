package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.krydo.mobile.data.StoredCredential

@Composable
fun CredentialsScreen(
    credentials: List<StoredCredential>,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("Credentials", style = MaterialTheme.typography.headlineMedium)
        Text("DEMO — Synthetic demo credentials, not real PII", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(credentials, key = { it.id }) { cred ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(cred.id) },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(cred.title, style = MaterialTheme.typography.titleMedium)
                        Text("Issuer: ${cred.issuerName}")
                        Text("Status: ${cred.status} · Expires: ${cred.expiresAt?.take(10) ?: "—"}")
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialDetailScreen(
    credential: StoredCredential?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(credential?.title ?: "Not found", style = MaterialTheme.typography.headlineSmall)
        if (credential != null) {
            Text("Claim type: ${credential.claimType}")
            Text("Issuer: ${credential.issuerName}")
            Text("Holder: ${credential.holderName}")
            Text("Status: ${credential.status}")
            Text("Summary: ${credential.displaySummary}")
            Text("Hash: ${credential.credentialHash.take(16)}…", style = MaterialTheme.typography.bodySmall)
        }
        androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") }
    }
}
