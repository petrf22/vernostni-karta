package io.github.petrf22.vernostnikarta.barcode

import com.google.zxing.BarcodeFormat

/** Code types the app can scan and show. The enum name is what gets stored and exported. */
enum class CodeFormat(val zxing: BarcodeFormat, val label: String, val is2D: Boolean) {
    QR_CODE(BarcodeFormat.QR_CODE, "QR Code", true),
    AZTEC(BarcodeFormat.AZTEC, "Aztec", true),
    DATA_MATRIX(BarcodeFormat.DATA_MATRIX, "Data Matrix", true),
    PDF_417(BarcodeFormat.PDF_417, "PDF417", true),
    EAN_13(BarcodeFormat.EAN_13, "EAN-13", false),
    EAN_8(BarcodeFormat.EAN_8, "EAN-8", false),
    UPC_A(BarcodeFormat.UPC_A, "UPC-A", false),
    UPC_E(BarcodeFormat.UPC_E, "UPC-E", false),
    CODE_128(BarcodeFormat.CODE_128, "Code 128", false),

    /**
     * Code 128 that starts with FNC1. The stored text is the data alone; variable-length fields
     * are separated by [GS1_SEPARATOR]. Must stay below [CODE_128] so [fromZxing] keeps
     * returning the plain one.
     */
    GS1_128(BarcodeFormat.CODE_128, "GS1-128", false),
    CODE_39(BarcodeFormat.CODE_39, "Code 39", false),
    CODE_93(BarcodeFormat.CODE_93, "Code 93", false),
    ITF(BarcodeFormat.ITF, "ITF", false),
    CODABAR(BarcodeFormat.CODABAR, "Codabar", false),
    ;

    companion object {
        /** ASCII group separator: how an FNC1 between GS1 fields is written in the stored text. */
        const val GS1_SEPARATOR = '\u001d'

        fun fromName(name: String?): CodeFormat? = entries.firstOrNull { it.name == name }
        fun fromZxing(format: BarcodeFormat): CodeFormat? = entries.firstOrNull { it.zxing == format }
    }
}
