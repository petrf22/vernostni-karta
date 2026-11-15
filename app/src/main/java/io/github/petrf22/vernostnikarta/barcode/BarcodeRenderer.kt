package io.github.petrf22.vernostnikarta.barcode

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object BarcodeRenderer {
    /** How ZXing's Code 128 writer expects FNC1 in its input. */
    private const val FNC1_ESCAPE = 'ñ'
    private val FNC_ESCAPES = 'ñ'..'ô'

    /**
     * Encodes [code] at its natural size: one cell per module, no quiet zone (the UI adds it).
     * 1D codes come back as a single row.
     *
     * @return null when the text cannot be expressed in the given format
     *   (wrong length, bad check digit, unsupported characters…)
     */
    fun encode(code: String, format: CodeFormat): BitMatrix? {
        if (code.isEmpty()) return null
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.MARGIN] = 0
        // Without an explicit charset (ECI) readers have to guess, and they guess wrong even for Latin-1.
        if (format.is2D && code.any { it.code > 0x7F }) {
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            // The default Data Matrix encoder ignores the charset and rejects anything above Latin-1.
            if (format == CodeFormat.DATA_MATRIX) hints[EncodeHintType.DATA_MATRIX_COMPACT] = true
        }
        var contents = code
        if (format.zxing == BarcodeFormat.CODE_128) {
            // ZXing's writer reads these as FNC1–FNC4; typed text must never turn into control codes.
            if (code.any { it in FNC_ESCAPES }) return null
            if (format == CodeFormat.GS1_128) {
                contents = FNC1_ESCAPE + code.replace(CodeFormat.GS1_SEPARATOR, FNC1_ESCAPE)
            }
        }
        return try {
            MultiFormatWriter().encode(contents, format.zxing, 0, 0, hints)
        } catch (e: Exception) {
            null
        }
    }

    fun toBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
