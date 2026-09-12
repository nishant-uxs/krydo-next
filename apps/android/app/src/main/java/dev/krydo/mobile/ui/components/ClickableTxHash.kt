package dev.krydo.mobile.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonElement
import dev.krydo.mobile.network.TxHashLookup
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun ClickableTxHash(
    txHash: String?,
    label: String = "Transaction",
    /** When known, pass transaction.data so only wallet-anchored txs link. */
    txData: JsonElement? = null,
    /** Force linkable (e.g. verified on-chain proof anchor). */
    forceOnChain: Boolean = false,
) {
    // Without txData, never open explorer for bare hashes (legacy random server hashes).
    val canOpen = when {
        txHash.isNullOrBlank() || TxHashLookup.isOffChain(txHash) -> false
        forceOnChain -> true
        txData != null -> TxHashLookup.isExplorerLinkable(txHash, txData)
        else -> false
    }

    if (!canOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label.uppercase(),
                color = KrydoColors.TextMuted,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = if (txHash.isNullOrBlank() || TxHashLookup.isOffChain(txHash)) {
                    "Off-chain / not on Stellar Expert"
                } else {
                    "Local record (not anchored on-chain)"
                },
                color = KrydoColors.TextSecondary,
                fontSize = 14.sp,
            )
            if (!txHash.isNullOrBlank() && !TxHashLookup.isOffChain(txHash) && !canOpen) {
                Text(
                    text = "${txHash.take(10)}…${txHash.takeLast(8)}",
                    color = KrydoColors.TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val short = "${txHash!!.take(10)}…${txHash.takeLast(8)}"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            color = KrydoColors.TextMuted,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
        )
        Text(
            text = short,
            color = KrydoColors.BrightBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(TxHashLookup.explorerUrl(txHash))),
                )
            },
        )
        Text(
            text = "Open on Stellar Expert",
            color = KrydoColors.Cyan.copy(alpha = 0.9f),
            fontSize = 11.sp,
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(TxHashLookup.explorerUrl(txHash))),
                )
            },
        )
    }
}
