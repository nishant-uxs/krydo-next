package dev.krydo.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.PillShape

@Composable
fun KrydoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    showArrow: Boolean = false,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 16.dp else 0.dp,
                shape = PillShape,
                ambientColor = KrydoColors.GlowBlue,
                spotColor = KrydoColors.GlowBlue,
            )
            .clip(PillShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        KrydoColors.ElectricBlue.copy(alpha = contentAlpha),
                        KrydoColors.BrightBlue.copy(alpha = contentAlpha),
                    ),
                ),
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = KrydoColors.TextPrimary.copy(alpha = contentAlpha),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = KrydoColors.TextPrimary.copy(alpha = contentAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (showArrow) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.92f * contentAlpha), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = KrydoColors.ElectricBlue,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun KrydoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(PillShape)
            .background(KrydoColors.CardSurface.copy(alpha = contentAlpha))
            .border(1.dp, KrydoColors.BorderBlue.copy(alpha = contentAlpha), PillShape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = KrydoColors.BrightBlue.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = KrydoColors.TextPrimary.copy(alpha = contentAlpha),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun KrydoTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (prefix != null) {
            Text(text = prefix, color = KrydoColors.TextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = KrydoColors.BrightBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
