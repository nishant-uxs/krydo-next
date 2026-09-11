package dev.krydo.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.krydo.mobile.R

@Composable
fun KrydoBrandHeader(
    subtitle: String? = "Prove claims carefully",
    logoSize: Dp = 88.dp,
    centered: Boolean = false,
) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.krydo_logo),
            contentDescription = "Krydo logo",
            modifier = Modifier
                .size(logoSize)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(text = "Krydo", style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.titleMedium)
        }
    }
}
