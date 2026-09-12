package dev.krydo.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.krydo.mobile.data.ClaimCategories
import dev.krydo.mobile.data.CredentialExpiry
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.ui.AppViewModel
import dev.krydo.mobile.ui.CredentialSortMode
import dev.krydo.mobile.ui.components.ClickableTxHash
import dev.krydo.mobile.ui.components.CredentialCard
import dev.krydo.mobile.ui.components.KrydoPrimaryButton
import dev.krydo.mobile.ui.components.KrydoPullRefresh
import dev.krydo.mobile.ui.components.KrydoSecondaryButton
import dev.krydo.mobile.ui.components.KrydoTopBar
import dev.krydo.mobile.ui.components.KrydoWordmark
import dev.krydo.mobile.ui.components.StatusPill
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.PillShape

@Composable
fun CredentialsScreen(
    viewModel: AppViewModel,
    onOpen: (String) -> Unit,
    onOpenRequest: () -> Unit = {},
    onOpenZk: (String) -> Unit = {},
) {
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val archivedIds by viewModel.archivedCredentialIds.collectAsStateWithLifecycle()
    val pinnedIds by viewModel.pinnedCredentialIds.collectAsStateWithLifecycle()
    val ui by viewModel.credentialsUi.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshCredentials()
    }

    val sections = viewModel.credentialSections(
        all = credentials,
        archivedIds = archivedIds,
        showArchived = ui.showArchived,
        searchQuery = ui.searchQuery,
        categoryFilter = ui.categoryFilter,
        pinnedIds = pinnedIds,
        sortMode = ui.sortMode,
    )
    val activeCount = credentials.count { it.id !in archivedIds }
    val archivedCount = credentials.count { it.id in archivedIds }
    var collapsedSections by remember { mutableStateOf(setOf<String>()) }
    var menuCredId by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KrydoColors.ElectricBlue,
        unfocusedBorderColor = KrydoColors.BorderSubtle,
        focusedTextColor = KrydoColors.TextPrimary,
        unfocusedTextColor = KrydoColors.TextPrimary,
        cursorColor = KrydoColors.ElectricBlue,
        focusedLabelColor = KrydoColors.BrightBlue,
        unfocusedLabelColor = KrydoColors.TextMuted,
        focusedContainerColor = KrydoColors.CardSurface,
        unfocusedContainerColor = KrydoColors.CardSurface,
    )

    KrydoPullRefresh(
        refreshing = ui.loading,
        onRefresh = viewModel::refreshCredentials,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KrydoColors.BackgroundPrimary)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    KrydoWordmark(subtitle = null)
                    Text(
                        text = "Your Credentials",
                        color = KrydoColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusPill(
                    text = if (ui.showArchived) "$archivedCount archived" else "$activeCount active",
                    dotColor = KrydoColors.ElectricBlue,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip(
                    text = "Active",
                    selected = !ui.showArchived,
                    onClick = { viewModel.setCredentialsShowArchived(false) },
                )
                TabChip(
                    text = "Archived",
                    selected = ui.showArchived,
                    onClick = { viewModel.setCredentialsShowArchived(true) },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = viewModel::setCredentialsSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search") },
                placeholder = { Text("Title, issuer, claim…") },
                shape = CardShape,
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterDropdown(
                    label = "Category",
                    value = ui.categoryFilter?.let { ClaimCategories.labelFor(it) } ?: "All categories",
                    modifier = Modifier.weight(1f),
                ) { dismiss ->
                    DropdownMenuItem(
                        text = { Text("All categories") },
                        onClick = {
                            viewModel.setCredentialsCategoryFilter(null)
                            dismiss()
                        },
                    )
                    ClaimCategories.knownClaimTypes().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(ClaimCategories.labelFor(type)) },
                            onClick = {
                                viewModel.setCredentialsCategoryFilter(type)
                                dismiss()
                            },
                        )
                    }
                }
                FilterDropdown(
                    label = "Sort",
                    value = ui.sortMode.label,
                    modifier = Modifier.weight(1f),
                ) { dismiss ->
                    CredentialSortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                viewModel.setCredentialsSortMode(mode)
                                dismiss()
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tip: long-press a card → archive, pin, copy, ZK",
                color = KrydoColors.TextMuted,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            KrydoSecondaryButton(
                text = "Request from issuer",
                onClick = onOpenRequest,
            )
            if (ui.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = KrydoColors.ElectricBlue)
                }
            }
            ui.error?.let {
                Text(
                    text = it,
                    color = KrydoColors.Error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (!ui.loading && sections.isEmpty() && ui.error == null) {
                Text(
                    text = when {
                        ui.searchQuery.isNotBlank() || ui.categoryFilter != null ->
                            "No credentials match this search/filter."
                        ui.showArchived ->
                            "No archived credentials. Long-press a card to archive."
                        else ->
                            "No active credentials. Request from an issuer, or check Archived."
                    },
                    color = KrydoColors.TextMuted,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { (category, list) ->
                    val collapsed = category in collapsedSections
                    item(key = "hdr-$category") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PillShape)
                                .clickable {
                                    collapsedSections = if (collapsed) {
                                        collapsedSections - category
                                    } else {
                                        collapsedSections + category
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${category.uppercase()} · ${list.size}",
                                color = KrydoColors.Cyan.copy(alpha = 0.95f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                            )
                            Icon(
                                imageVector = if (collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                contentDescription = if (collapsed) "Expand" else "Collapse",
                                tint = KrydoColors.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    if (!collapsed) {
                        items(list, key = { it.id }) { cred ->
                            Box {
                                CredentialCard(
                                    credential = cred,
                                    pinned = cred.id in pinnedIds,
                                    onClick = { onOpen(cred.id) },
                                    onLongClick = { menuCredId = cred.id },
                                )
                                DropdownMenu(
                                    expanded = menuCredId == cred.id,
                                    onDismissRequest = { menuCredId = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open") },
                                        onClick = {
                                            menuCredId = null
                                            onOpen(cred.id)
                                        },
                                    )
                                    if (!ui.showArchived) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (cred.id in pinnedIds) "Unpin" else "Pin to top")
                                            },
                                            onClick = {
                                                val wasPinned = cred.id in pinnedIds
                                                menuCredId = null
                                                if (wasPinned) viewModel.unpinCredential(cred.id)
                                                else viewModel.pinCredential(cred.id)
                                                Toast.makeText(
                                                    context,
                                                    if (wasPinned) "Unpinned" else "Pinned",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Generate ZK proof") },
                                            onClick = {
                                                menuCredId = null
                                                onOpenZk(cred.id)
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (ui.showArchived) "Restore to Active" else "Archive")
                                        },
                                        onClick = {
                                            menuCredId = null
                                            if (ui.showArchived) {
                                                viewModel.unarchiveCredential(cred.id)
                                                Toast.makeText(context, "Restored", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.archiveCredential(cred.id)
                                                Toast.makeText(context, "Archived", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Copy hash") },
                                        onClick = {
                                            menuCredId = null
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(
                                                ClipData.newPlainText("credential hash", cred.credentialHash),
                                            )
                                            Toast.makeText(context, "Hash copied", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(KrydoColors.CardSurface)
                .border(1.dp, KrydoColors.BorderSubtle, CardShape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label.uppercase(),
                color = KrydoColors.TextMuted,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    color = KrydoColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = KrydoColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            menuContent { expanded = false }
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) KrydoColors.ElectricBlue.copy(alpha = 0.22f) else KrydoColors.CardSurface
    val border = if (selected) KrydoColors.ElectricBlue else KrydoColors.BorderSubtle
    val fg = if (selected) KrydoColors.TextPrimary else KrydoColors.TextMuted
    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun CredentialDetailScreen(
    credential: StoredCredential?,
    archived: Boolean,
    pinned: Boolean,
    onArchiveToggle: () -> Unit,
    onPinToggle: () -> Unit,
    onProve: () -> Unit,
    onZkProof: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KrydoColors.BackgroundPrimary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KrydoTopBar(
            title = credential?.title ?: "Credential",
            subtitle = credential?.let { ClaimCategories.labelFor(it.claimType) },
            onBack = onBack,
        )
        if (credential != null) {
            DetailLine("Category", ClaimCategories.labelFor(credential.claimType))
            DetailLine("Claim type", credential.claimType)
            DetailLine("Claim value", credential.claimValue ?: "—")
            DetailLine("Issuer", credential.issuerName)
            DetailLine("Issuer address", credential.issuerAddress)
            DetailLine("Holder", credential.holderAddress)
            DetailLine("Status", credential.status)
            DetailLine("Expiry", CredentialExpiry.label(credential.expiresAt))
            DetailLine("Summary", credential.displaySummary)
            DetailLine("Hash", credential.credentialHash)
            ClickableTxHash(txHash = credential.onChainTxHash, label = "Issue transaction", forceOnChain = true)
            Spacer(modifier = Modifier.height(4.dp))
            if (!archived) {
                KrydoPrimaryButton(text = "Generate ZK proof", onClick = onZkProof)
                KrydoSecondaryButton(text = "Open Prove", onClick = onProve)
                KrydoSecondaryButton(
                    text = if (pinned) "Unpin" else "Pin to top",
                    onClick = onPinToggle,
                )
            }
            if (archived) {
                KrydoPrimaryButton(text = "Restore to Active", onClick = onArchiveToggle)
            } else {
                KrydoSecondaryButton(text = "Archive", onClick = onArchiveToggle)
            }
            KrydoSecondaryButton(
                text = "Copy credential hash",
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("credential hash", credential.credentialHash))
                    Toast.makeText(context, "Hash copied", Toast.LENGTH_SHORT).show()
                },
            )
        } else {
            Text(
                text = "Credential not found.",
                color = KrydoColors.TextMuted,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label.uppercase(), color = KrydoColors.TextMuted, fontSize = 11.sp, letterSpacing = 0.8.sp)
        Text(text = value, color = KrydoColors.TextSecondary, fontSize = 14.sp)
    }
}
