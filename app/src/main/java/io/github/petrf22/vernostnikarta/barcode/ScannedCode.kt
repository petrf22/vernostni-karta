package io.github.petrf22.vernostnikarta.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType

/** Turns what ZXing read into the text and format the app stores, without losing anything. */
object ScannedCode {
    private const val GS1_128_IDENTIFIER = "]C1"

    /**
     * Hints every reader in the app must use. Without [DecodeHintType.ASSUME_GS1] the Code 128
     * reader silently drops FNC1, which merges the fields of a GS1-128 code.
     */
    val DECODE_HINTS: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to CodeFormat.entries.map { it.zxing }.distinct(),
        DecodeHintType.ASSUME_GS1 to true,
    )

    /** @return the exact content and its format, or null when the result is empty or not supported */
    fun fromResult(result: Result): Pair<String, CodeFormat>? {
        val text = result.text ?: return null
        // The symbology identifier tells GS1-128 from a plain Code 128 whose data starts with "]C1".
        val isGs1 = result.barcodeFormat == BarcodeFormat.CODE_128 &&
            result.resultMetadata?.get(ResultMetadataType.SYMBOLOGY_IDENTIFIER) == GS1_128_IDENTIFIER &&
            text.startsWith(GS1_128_IDENTIFIER)
        val format = if (isGs1) CodeFormat.GS1_128 else CodeFormat.fromZxing(result.barcodeFormat) ?: return null
        val content = if (isGs1) text.removePrefix(GS1_128_IDENTIFIER) else text
        return if (content.isEmpty()) null else content to format
    }
}
