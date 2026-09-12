package dev.krydo.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dev.krydo.mobile.ui.theme.KrydoColors

@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220,
) {
    val bitmap = remember(content, sizeDp) {
        encodeQrBitmap(content, sizePx = sizeDp * 3)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KrydoColors.TextPrimary, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR code",
            modifier = Modifier.size(sizeDp.dp),
        )
    }
}

private fun encodeQrBitmap(content: String, sizePx: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (matrix[x, y]) 0xFF0B1220.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bmp
}
