package io.github.petrf22.vernostnikarta.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.petrf22.vernostnikarta.R
import io.github.petrf22.vernostnikarta.barcode.BarcodeRenderer
import io.github.petrf22.vernostnikarta.barcode.CodeFormat
import kotlin.math.floor
import kotlin.math.min

/**
 * Draws the code as large as fits into the given space. Modules are scaled by a whole number of
 * pixels whenever possible, so bars keep exact widths and scanners read them reliably.
 * Expects a white background.
 */
@Composable
fun BarcodeView(code: String, format: CodeFormat, modifier: Modifier = Modifier) {
    val image = remember(code, format) {
        BarcodeRenderer.encode(code, format)?.let { BarcodeRenderer.toBitmap(it).asImageBitmap() }
    }
    if (image == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            // Nothing typed yet is not an error.
            if (code.isNotEmpty()) Text(
                text = stringResource(R.string.error_code_invalid),
                color = Color(0xFFB3261E),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    Canvas(modifier) {
        val columns = image.width
        val rows = image.height
        fun wholeIfPossible(scale: Float) = if (scale >= 1f) floor(scale) else scale
        val dstWidth: Int
        val dstHeight: Int
        if (rows == 1) {
            // 1D: exact bar widths, bars as tall as the space allows.
            dstWidth = (columns * wholeIfPossible(size.width / columns)).toInt()
            dstHeight = size.height.toInt()
        } else {
            val scale = wholeIfPossible(min(size.width / columns, size.height / rows))
            dstWidth = (columns * scale).toInt()
            dstHeight = (rows * scale).toInt()
        }
        if (dstWidth <= 0 || dstHeight <= 0) return@Canvas
        drawImage(
            image = image,
            dstOffset = IntOffset(((size.width - dstWidth) / 2).toInt(), ((size.height - dstHeight) / 2).toInt()),
            dstSize = IntSize(dstWidth, dstHeight),
            filterQuality = FilterQuality.None,
        )
    }
}
