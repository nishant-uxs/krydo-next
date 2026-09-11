package dev.krydo.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.ui.theme.CardShape
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.PillShape

@Composable
fun CredentialCard(
    credential: StoredCredential,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val verified = credential.status.equals("active", ignoreCase = true) ||
        credential.status.equals("verified", ignoreCase = true) ||
        credential.status.equals("issued", ignoreCase = true)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrydoColors.CardSurface)
            .border(1.dp, KrydoColors.BorderSubtle, CardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(KrydoColors.ElectricBlue)
                .align(Alignment.CenterVertically),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(KrydoColors.SurfaceElevated, CircleShape)
                .border(1.dp, KrydoColors.BorderBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (credential.claimType.contains("age", ignoreCase = true)) {
                    Icons.Outlined.Shield
                } else {
                    Icons.Outlined.Badge
                },
                contentDescription = null,
                tint = KrydoColors.BrightBlue,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = credential.title,
                    color = KrydoColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(
                    text = if (verified) "Verified" else credential.status,
                    tint = if (verified) KrydoColors.Success else KrydoColors.Warning,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = credential.displaySummary.ifBlank { credential.claimType },
                color = KrydoColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Expires ${credential.expiresAt?.take(10) ?: "—"}",
                color = KrydoColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), PillShape)
            .border(1.dp, tint.copy(alpha = 0.35f), PillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ActivityRow(
    title: String,
    subtitle: String,
    time: String,
    iconTint: Color = KrydoColors.Success,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(KrydoColors.SurfaceElevated, CircleShape)
                .border(1.dp, KrydoColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = KrydoColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = KrydoColors.TextMuted, fontSize = 12.sp)
        }
        Text(text = time, color = KrydoColors.TextMuted, fontSize = 12.sp)
    }
}
