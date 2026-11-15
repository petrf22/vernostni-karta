package io.github.petrf22.vernostnikarta

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import io.github.petrf22.vernostnikarta.barcode.BarcodeRenderer
import io.github.petrf22.vernostnikarta.barcode.CodeFormat
import io.github.petrf22.vernostnikarta.barcode.ScannedCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeRendererTest {
    @Test
    fun ean13IsOneRowOf95Modules() {
        val matrix = BarcodeRenderer.encode("5901234123457", CodeFormat.EAN_13)!!
        assertEquals(95, matrix.width)
        assertEquals(1, matrix.height)
    }

    @Test
    fun ean13WithWrongCheckDigitIsRejected() {
        assertNull(BarcodeRenderer.encode("5901234123450", CodeFormat.EAN_13))
        assertNull(BarcodeRenderer.encode("abc", CodeFormat.EAN_13))
    }

    @Test
    fun emptyCodeIsRejected() {
        CodeFormat.entries.forEach { assertNull(it.name, BarcodeRenderer.encode("", it)) }
    }

    @Test
    fun qrIsSquareAndTakesUnicode() {
        val matrix = BarcodeRenderer.encode("Žluťoučký kůň 123", CodeFormat.QR_CODE)!!
        assertEquals(matrix.width, matrix.height)
    }

    /** What the app draws must read back as the same text in the same format. */
    @Test
    fun everyFormatReadsBack() {
        val samples = mapOf(
            CodeFormat.QR_CODE to "https://example.com/card/123456",
            CodeFormat.AZTEC to "AZTEC-123456",
            CodeFormat.DATA_MATRIX to "DM-123456",
            CodeFormat.PDF_417 to "PDF417-123456",
            CodeFormat.EAN_13 to "5901234123457",
            CodeFormat.EAN_8 to "96385074",
            CodeFormat.UPC_A to "036000291452",
            CodeFormat.UPC_E to "01234565",
            CodeFormat.CODE_128 to "Card-2401234567890",
            // Two variable-length fields: the separator between them must survive.
            CodeFormat.GS1_128 to "010950110153000310AB123\u001d17251231",
            CodeFormat.CODE_39 to "CARD 123",
            CodeFormat.CODE_93 to "CARD93",
            CodeFormat.ITF to "30712345000010",
            CodeFormat.CODABAR to "123456789",
        )
        assertEquals(CodeFormat.entries.toSet(), samples.keys)
        for ((format, text) in samples) {
            assertEquals(format.name, text to format, readBack(text, format))
        }
    }

    /** No charset guessing on the reader's side: Latin-1 and other Unicode come back unchanged. */
    @Test
    fun nonAsciiTextReadsBackInEvery2DFormat() {
        for (format in CodeFormat.entries.filter { it.is2D }) {
            for (text in listOf("äöü", "Žluťoučký kůň 123")) {
                assertEquals(format.name, text to format, readBack(text, format))
            }
        }
    }

    @Test
    fun surroundingSpacesSurvive() {
        for (format in listOf(CodeFormat.QR_CODE, CodeFormat.CODE_128)) {
            assertEquals(format.name, "  12345 " to format, readBack("  12345 ", format))
        }
    }

    @Test
    fun plainCode128StartingWithGs1IdentifierStaysPlain() {
        assertEquals("]C1abc" to CodeFormat.CODE_128, readBack("]C1abc", CodeFormat.CODE_128))
    }

    /** ZXing's writer would turn these characters into FNC1–FNC4. */
    @Test
    fun fncEscapesAreRejectedInCode128() {
        for (format in listOf(CodeFormat.CODE_128, CodeFormat.GS1_128)) {
            for (escape in "\u00f1\u00f2\u00f3\u00f4") {
                assertNull(format.name, BarcodeRenderer.encode("12${escape}34", format))
            }
        }
    }

    /** Draws the code and reads it the way the scanner does. */
    private fun readBack(text: String, format: CodeFormat): Pair<String, CodeFormat>? {
        val matrix = BarcodeRenderer.encode(text, format)
        assertNotNull("${format.name} must encode", matrix)
        val result = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(MatrixSource(matrix!!, scale = 4, margin = 40))),
            ScannedCode.DECODE_HINTS + (DecodeHintType.POSSIBLE_FORMATS to listOf(format.zxing)),
        )
        return ScannedCode.fromResult(result)
    }

    /** The matrix blown up the way the screen shows it: whole-pixel modules on a white field. */
    private class MatrixSource(matrix: BitMatrix, scale: Int, margin: Int) : LuminanceSource(
        matrix.width * scale + 2 * margin,
        (if (matrix.height == 1) 60 else matrix.height * scale) + 2 * margin,
    ) {
        private val pixels = ByteArray(width * height) { 0xFF.toByte() }

        init {
            val bodyHeight = height - 2 * margin
            for (y in 0 until bodyHeight) {
                val row = if (matrix.height == 1) 0 else y / scale
                for (x in 0 until matrix.width * scale) {
                    if (matrix[x / scale, row]) pixels[(y + margin) * width + x + margin] = 0
                }
            }
        }

        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val out = if (row != null && row.size >= width) row else ByteArray(width)
            System.arraycopy(pixels, y * width, out, 0, width)
            return out
        }

        override fun getMatrix(): ByteArray = pixels
    }
}
