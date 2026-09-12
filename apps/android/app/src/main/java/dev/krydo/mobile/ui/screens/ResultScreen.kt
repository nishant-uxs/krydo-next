package dev.krydo.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.IdentityCore
import dev.krydo.mobile.ui.components.IdentityCoreMode
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoTopBar
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun ResultScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.prove.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.presentation) {
        if (state.presentation != null && state.verifyResult == null && !state.loading) {
            viewModel.verifyPresentation()
        }
    }

    val result = state.verifyResult
    val valid = result?.valid == true
    val failed = result != null && !valid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KrydoTopBar(
            title = "Result",
            subtitle = "Presentation verification",
            onBack = onBack,
        )
        StatusPill(
            text = when {
                valid -> "Verified presentation"
                failed -> "Verification failed"
                else -> "Verifying…"
            },
            dotColor = when {
                valid -> KrydoColors.Success
                failed -> KrydoColors.Error
                else -> KrydoColors.Cyan
            },
            textColor = when {
                valid -> KrydoColors.Success
                failed -> KrydoColors.Error
                else -> KrydoColors.Cyan
            },
        )

        IdentityCore(
            coreSize = 180.dp,
            mode = if (valid) IdentityCoreMode.Verified else IdentityCoreMode.Idle,
            label = when {
                valid -> "OK"
                failed -> "FAIL"
                else -> "…"
            },
            icon = if (failed) Icons.Outlined.Close else Icons.Outlined.Check,
        )

        if (state.loading) {
            CircularProgressIndicator(color = KrydoColors.ElectricBlue)
        }
        state.error?.let {
            Text(text = it, color = KrydoColors.Error, fontSize = 13.sp)
        }

        state.verifyResult?.let { result ->
            Text(
                text = if (result.valid) "Verified" else "Not verified",
                color = KrydoColors.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = result.message,
                color = KrydoColors.TextSecondary,
                fontSize = 14.sp,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KrydoColors.CardSurface, CardShape)
                    .border(1.dp, KrydoColors.BorderBlue, CardShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "VALIDATION CHECKS",
                    color = KrydoColors.TextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
                result.checks?.forEach { (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = k, color = KrydoColors.TextSecondary, fontSize = 13.sp)
                        Text(
                            text = if (v) "pass" else "fail",
                            color = if (v) KrydoColors.Success else KrydoColors.Error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        KrydoPrimaryButton(text = "Done & Return", onClick = onBack, showArrow = true)
        KrydoSecondaryButton(
            text = "Share Proof Receipt",
            onClick = {
                val pretty = viewModel.presentationPretty().orEmpty()
                val result = state.verifyResult
                val checks = result?.checks?.entries
                    ?.joinToString("\n") { (k, v) -> "$k: ${if (v) "pass" else "fail"}" }
                    .orEmpty()
                val body = buildString {
                    appendLine("Krydo presentation receipt")
                    appendLine("valid: ${result?.valid}")
                    result?.message?.takeIf { it.isNotBlank() }?.let {
                        appendLine("message: $it")
                    }
                    if (checks.isNotBlank()) {
                        appendLine()
                        appendLine("Checks:")
                        appendLine(checks)
                    }
                    if (pretty.isNotBlank()) {
                        appendLine()
                        appendLine("Presentation JSON:")
                        append(pretty)
                    }
                }.trim()
                if (body.isBlank()) return@KrydoSecondaryButton
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Krydo proof receipt")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(intent, "Share proof receipt"))
            },
            enabled = state.presentation != null || state.verifyResult != null,
        )
    }
}
