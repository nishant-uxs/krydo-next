package dev.krydo.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.krydo.mobile.ui.theme.FloatingNavShape
import dev.krydo.mobile.ui.theme.KrydoColors

data class FloatingNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun FloatingBottomNav(
    items: List<FloatingNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = items.size >= 5

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(18.dp, FloatingNavShape, ambientColor = KrydoColors.GlowBlue, spotColor = KrydoColors.GlowBlue)
                .clip(FloatingNavShape)
                .background(KrydoColors.BackgroundSecondary.copy(alpha = 0.96f))
                .border(1.dp, KrydoColors.BorderSubtle, FloatingNavShape)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val tint = if (selected) KrydoColors.ElectricBlue else KrydoColors.TextMuted
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(FloatingNavShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onNavigate(item.route) },
                        )
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (selected) 34.dp else 28.dp)
                                .then(
                                    if (selected) {
                                        Modifier.background(
                                            KrydoColors.ElectricBlue.copy(alpha = 0.16f),
                                            CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = tint,
                                modifier = Modifier.size(if (compact) 20.dp else 22.dp),
                            )
                        }
                        Text(
                            text = if (compact && item.label.length > 6) {
                                item.label.take(5) + "…"
                            } else {
                                item.label
                            },
                            color = tint,
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
