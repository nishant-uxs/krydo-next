package dev.krydo.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.krydo.mobile.ui.theme.KrydoColors
import dev.krydo.mobile.ui.theme.PillShape
import kotlin.math.cos
import kotlin.math.sin

enum class IdentityCoreMode {
    Idle,
    Onboarding,
    Verified,
}

@Composable
fun IdentityCore(
    modifier: Modifier = Modifier,
    coreSize: Dp = 220.dp,
    mode: IdentityCoreMode = IdentityCoreMode.Idle,
    label: String = "ACTIVE",
    icon: ImageVector = Icons.Outlined.Fingerprint,
) {
    val transition = rememberInfiniteTransition(label = "identity-core")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit",
    )
    val ringColor = when (mode) {
        IdentityCoreMode.Verified -> KrydoColors.Success.copy(alpha = 0.55f)
        else -> KrydoColors.ElectricBlue.copy(alpha = 0.55f)
    }
    val coreIconTint = when (mode) {
        IdentityCoreMode.Verified -> KrydoColors.Success
        IdentityCoreMode.Onboarding -> KrydoColors.BrightBlue
        IdentityCoreMode.Idle -> KrydoColors.ElectricBlue
    }

    Box(
        modifier = modifier
            .size(coreSize)
            .scale(breathe),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val maxR = this.size.minDimension / 2f
            val rings = listOf(0.42f, 0.58f, 0.74f, 0.9f)
            rings.forEachIndexed { index, frac ->
                drawCircle(
                    color = ringColor.copy(alpha = 0.18f + index * 0.08f),
                    radius = maxR * frac,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.6f),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        KrydoColors.ElectricBlue.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = maxR,
                ),
                radius = maxR,
                center = Offset(cx, cy),
            )
            listOf(0f, 120f, 240f).forEach { base ->
                val angle = Math.toRadians((orbit + base).toDouble())
                val r = maxR * 0.74f
                val x = cx + (cos(angle) * r).toFloat()
                val y = cy + (sin(angle) * r).toFloat()
                drawCircle(
                    color = KrydoColors.Cyan.copy(alpha = 0.9f),
                    radius = 4.5f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = KrydoColors.Cyan.copy(alpha = 0.25f),
                    radius = 10f,
                    center = Offset(x, y),
                )
            }
            drawArc(
                color = KrydoColors.BrightBlue.copy(alpha = 0.65f),
                startAngle = orbit,
                sweepAngle = 48f,
                useCenter = false,
                topLeft = Offset(cx - maxR * 0.58f, cy - maxR * 0.58f),
                size = Size(maxR * 1.16f, maxR * 1.16f),
                style = Stroke(width = 2.4f, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KrydoColors.SurfaceElevated,
                                KrydoColors.BackgroundPrimary,
                            ),
                        ),
                        shape = CircleShape,
                    )
                    .border(1.dp, KrydoColors.BorderBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (mode == IdentityCoreMode.Onboarding) {
                        Icons.Outlined.Shield
                    } else {
                        icon
                    },
                    contentDescription = null,
                    tint = coreIconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
            if (mode != IdentityCoreMode.Onboarding) {
                Box(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    color = coreIconTint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = KrydoColors.ElectricBlue,
    textColor: Color = KrydoColors.TextSecondary,
) {
    Row(
        modifier = modifier
            .background(KrydoColors.CardSurface.copy(alpha = 0.85f), PillShape)
            .border(1.dp, KrydoColors.BorderSubtle, PillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = KrydoColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        trailing?.invoke()
    }
}

@Composable
fun KrydoWordmark(
    modifier: Modifier = Modifier,
    subtitle: String? = "VERIFIED IDENTITY",
) {
    Column(modifier = modifier) {
        Text(
            text = "KRYDO",
            color = KrydoColors.BrightBlue,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = KrydoColors.Cyan.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )
        }
    }
}
