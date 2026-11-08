package com.cinema4d.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun TicketQrCode(
    content: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 140.dp,
) {
    val matrix = rememberQrMatrix(content)
    Canvas(modifier = modifier.size(size)) {
        val moduleCount = matrix.width
        val moduleSize = size.toPx() / moduleCount
        val dark = Color(0xFF10131A)
        val light = Color(0xFFF2F4F8)
        drawRect(light)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = dark,
                        topLeft = Offset(x * moduleSize, y * moduleSize),
                        size = androidx.compose.ui.geometry.Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberQrMatrix(content: String): com.google.zxing.common.BitMatrix {
    return androidx.compose.runtime.remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 440, 440)
    }
}
