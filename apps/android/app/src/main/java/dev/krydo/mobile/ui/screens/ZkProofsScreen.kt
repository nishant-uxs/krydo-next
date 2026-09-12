package dev.krydo.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.data.ZkProofRepository
import dev.krydo.mobile.network.TxHashLookup
import dev.krydo.mobile.network.ZkProofDto
import dev.krydo.mobile.network.ZkShareLinks
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.components.ClickableTxHash
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.QrCodeImage
import dev.krydo.mobile.ui.components.SectionHeader
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun ZkProofsScreen(
    viewModel: AppViewModel,
) {
    val credentials by viewModel.activeCredentials.collectAsStateWithLifecycle()
    val proofs by viewModel.zkProofs.collectAsStateWithLifecycle()
    val ui by viewModel.zkUi.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val active = credentials.filter {
        it.status.equals("active", ignoreCase = true)
    }

    var selectedCred by remember { mutableStateOf<StoredCredential?>(null) }
    var proofType by remember { mutableStateOf("range_above") }
    var threshold by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshCredentials()
        viewModel.refreshZkProofs()
    }

    LaunchedEffect(active, ui.preferredCredentialId) {
        val prefer = ui.preferredCredentialId
        if (!prefer.isNullOrBlank()) {
            val match = active.firstOrNull { it.id == prefer }
            if (match != null) {
                selectedCred = match
                viewModel.consumeZkPreferredCredential()
                return@LaunchedEffect
            }
            if (active.isEmpty()) return@LaunchedEffect
            viewModel.consumeZkPreferredCredential()
        }
        if (selectedCred == null || active.none { it.id == selectedCred?.id }) {
            selectedCred = active.firstOrNull()
        }
    }

    ui.shareProofId?.let { proofId ->
        ShareProofDialog(
            proofId = proofId,
            onDismiss = viewModel::clearShareProof,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KrydoWordmark(subtitle = null)
        Text(
            text = "Zero-Knowledge Proofs",
            color = KrydoColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Pick a proof type, set the claim, then choose a credential and generate a shareable QR. Proofs are created on the Krydo API (server-side sigma proofs) — share only the verify link.",
            color = KrydoColors.TextMuted,
            fontSize = 13.sp,
        )

        StatusPill(
            text = "${proofs.size} proofs · ${active.size} active creds",
            dotColor = KrydoColors.ElectricBlue,
        )

        ui.error?.let { err ->
            Text(text = err, color = KrydoColors.Error, fontSize = 13.sp)
        }
        ui.successMessage?.let { ok ->
            Text(text = ok, color = KrydoColors.Success, fontSize = 13.sp)
        }

        // 1) Proof type + inputs first (so fields aren't buried under credential list)
        SectionHeader(title = "1. Proof type")
        ZkProofRepository.proofTypeLabels.forEach { (key, label) ->
            ProofTypeChip(
                label = label,
                selected = proofType == key,
                onClick = { proofType = key },
            )
        }

        if (proofType == "range_above" || proofType == "range_below") {
            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (proofType == "range_above") "Threshold (prove value ≥)" else "Threshold (prove value ≤)") },
                placeholder = { Text("e.g. 700") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = zkFieldColors(),
                singleLine = true,
            )
        }

        if (proofType == "equality") {
            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exact target value") },
                colors = zkFieldColors(),
                singleLine = true,
            )
        }

        // 2) Credential
        SectionHeader(title = "2. Credential")
        if (active.isEmpty()) {
            Text(
                text = "No active credentials. Request one first, then generate a ZK proof.",
                color = KrydoColors.TextMuted,
                fontSize = 13.sp,
            )
        } else {
            active.forEach { cred ->
                CredPickRow(
                    credential = cred,
                    selected = selectedCred?.id == cred.id,
                    onClick = { selectedCred = cred },
                )
            }
        }

        // 3) Generate
        formError?.let {
            Text(text = it, color = KrydoColors.Error, fontSize = 13.sp)
        }
        KrydoPrimaryButton(
            text = if (ui.generating) "Generating…" else "3. Generate & share QR",
            onClick = {
                val cred = selectedCred
                if (cred == null) {
                    formError = "Pick a credential first."
                    return@KrydoPrimaryButton
                }
                val thr = threshold.toDoubleOrNull()
                if ((proofType == "range_above" || proofType == "range_below") && thr == null) {
                    formError = "Enter a valid numeric threshold."
                    return@KrydoPrimaryButton
                }
                if (proofType == "equality" && targetValue.isBlank()) {
                    formError = "Enter the exact target value."
                    return@KrydoPrimaryButton
                }
                formError = null
                viewModel.generateZkProof(
                    credentialId = cred.id,
                    proofType = proofType,
                    threshold = thr,
                    targetValue = targetValue,
                )
            },
            enabled = !ui.generating && selectedCred != null,
            leadingIcon = Icons.Outlined.Fingerprint,
        )

        ui.lastProof?.let { proof ->
            GeneratedProofCard(
                proof = proof,
                onOpenShare = { viewModel.openShareProof(proof.id) },
                onOpenLink = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(ZkShareLinks.verifyUrl(proof.id))),
                    )
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your proofs",
                color = KrydoColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (ui.loading) "…" else "Refresh",
                color = KrydoColors.BrightBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = !ui.loading) {
                    viewModel.refreshZkProofs()
                },
            )
        }

        if (ui.loading && proofs.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = KrydoColors.ElectricBlue)
            }
        }

        if (!ui.loading && proofs.isEmpty()) {
            Text(
                text = "No proofs yet. Generate one above.",
                color = KrydoColors.TextMuted,
                fontSize = 13.sp,
            )
        }

        proofs.forEach { proof ->
            ProofCard(
                proof = proof,
                onOpenShare = { viewModel.openShareProof(proof.id) },
                onOpenLink = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(ZkShareLinks.verifyUrl(proof.id))),
                    )
                },
            )
        }
    }
}

@Composable
private fun ShareProofDialog(
    proofId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val url = ZkShareLinks.verifyUrl(proofId)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(KrydoColors.BackgroundSecondary)
                .border(1.dp, KrydoColors.BorderSubtle, CardShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Share ZK proof",
                    color = KrydoColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = KrydoColors.TextMuted)
                }
            }
            Text(
                text = "Verifier scans this QR (no login needed) or opens the link.",
                color = KrydoColors.TextMuted,
                fontSize = 13.sp,
            )
            QrCodeImage(content = url, sizeDp = 220)
            Text(
                text = url,
                color = KrydoColors.BrightBlue,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KrydoSecondaryButton(
                    text = "Copy link",
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Krydo ZK proof", url))
                    },
                    leadingIcon = Icons.Outlined.ContentCopy,
                    modifier = Modifier.weight(1f),
                )
                KrydoPrimaryButton(
                    text = "Share",
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Krydo ZK proof")
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(send, "Share proof"))
                    },
                    leadingIcon = Icons.Outlined.OpenInNew,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GeneratedProofCard(
    proof: ZkProofDto,
    onOpenShare: () -> Unit,
    onOpenLink: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrydoColors.SurfaceElevated)
            .border(1.5.dp, KrydoColors.ElectricBlue, CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Proof ready",
            color = KrydoColors.Success,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(
            text = ZkProofRepository.labelFor(proof.proofType),
            color = KrydoColors.TextPrimary,
            fontSize = 14.sp,
        )
        Text(
            text = ZkShareLinks.verifyUrl(proof.id),
            color = KrydoColors.BrightBlue,
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onOpenLink),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KrydoPrimaryButton(
                text = "Show QR",
                onClick = onOpenShare,
                leadingIcon = Icons.Outlined.QrCode2,
                modifier = Modifier.weight(1f),
            )
            KrydoSecondaryButton(
                text = "Open verify",
                onClick = onOpenLink,
                leadingIcon = Icons.Outlined.OpenInNew,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun zkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KrydoColors.ElectricBlue,
    unfocusedBorderColor = KrydoColors.BorderSubtle,
    focusedTextColor = KrydoColors.TextPrimary,
    unfocusedTextColor = KrydoColors.TextPrimary,
    focusedLabelColor = KrydoColors.TextSecondary,
    unfocusedLabelColor = KrydoColors.TextMuted,
    cursorColor = KrydoColors.ElectricBlue,
)

@Composable
private fun CredPickRow(
    credential: StoredCredential,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (selected) KrydoColors.SurfaceElevated else KrydoColors.CardSurface)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle,
                CardShape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .size(40.dp)
                .background(KrydoColors.SurfaceElevated, CircleShape)
                .border(1.dp, KrydoColors.BorderBlue, CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = KrydoColors.BrightBlue)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = credential.title,
                color = KrydoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${credential.claimType} · ${credential.issuerName}",
                color = KrydoColors.TextMuted,
                fontSize = 12.sp,
            )
            if (!credential.claimValue.isNullOrBlank()) {
                Text(
                    text = "Value: ${credential.claimValue}",
                    color = KrydoColors.Cyan.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (selected) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = KrydoColors.Success)
        }
    }
}

@Composable
private fun ProofTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = KrydoColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) KrydoColors.SurfaceElevated else KrydoColors.CardSurface)
            .border(
                1.dp,
                if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun ProofCard(
    proof: ZkProofDto,
    onOpenShare: () -> Unit,
    onOpenLink: () -> Unit,
) {
    val ok = proof.verified
    val commit = proof.commitment.orEmpty()
    val tx = proof.onChainTxHash
    val url = ZkShareLinks.verifyUrl(proof.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrydoColors.CardSurface)
            .border(1.dp, KrydoColors.BorderSubtle, CardShape)
            .clickable(onClick = onOpenShare)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = if (ok) KrydoColors.Success else KrydoColors.Warning,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = ZkProofRepository.labelFor(proof.proofType),
                color = KrydoColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            StatusPill(
                text = if (ok) "verified" else "generated",
                dotColor = if (ok) KrydoColors.Success else KrydoColors.Warning,
            )
        }
        Text(
            text = proof.claimSummary ?: proof.claimType ?: "credential ${proof.credentialId.take(8)}…",
            color = KrydoColors.TextMuted,
            fontSize = 12.sp,
        )
        Text(
            text = url,
            color = KrydoColors.BrightBlue,
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onOpenLink),
        )
        Text(
            text = "id ${proof.id.take(8)}…" + if (commit.isNotBlank()) " · commit ${commit.take(10)}…" else "",
            color = KrydoColors.Cyan.copy(alpha = 0.85f),
            fontSize = 11.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KrydoSecondaryButton(
                text = "QR",
                onClick = onOpenShare,
                leadingIcon = Icons.Outlined.QrCode2,
                modifier = Modifier.weight(1f),
            )
            KrydoSecondaryButton(
                text = "Open",
                onClick = onOpenLink,
                leadingIcon = Icons.Outlined.OpenInNew,
                modifier = Modifier.weight(1f),
            )
        }
        if (!tx.isNullOrBlank() && !TxHashLookup.isOffChain(tx)) {
            ClickableTxHash(txHash = tx, label = "Anchor transaction", forceOnChain = true)
        }
    }
}
