package dev.krydo.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.krydo.mobile.ui.components.KrydoBrandHeader

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        KrydoBrandHeader(centered = true, logoSize = 112.dp)
        Text(
            text = "Krydo lets you prove credential claims without unnecessarily revealing the underlying data.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Native Android wallet", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "This app talks to your hosted Krydo backend (Render). " +
                        "Local DEMO proofs are labeled clearly. Device-only ZK is a later hardening step.",
                )
            }
        }
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "What you can do now", style = MaterialTheme.typography.titleSmall)
                Text(text = "1. Connect to your Render API URL in Settings")
                Text(text = "2. Browse credentials")
                Text(text = "3. Open a presentation request")
                Text(text = "4. Create + verify a presentation")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Continue")
        }
    }
}
