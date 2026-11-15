package io.github.petrf22.vernostnikarta.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/** Finds a code in a picture the user picked (a photo or a screenshot of the card). */
object ImageBarcodeDecoder {
    private const val MAX_SIDE = 1600

    fun decode(context: Context, uri: Uri): Pair<String, CodeFormat>? {
        val bitmap = load(context, uri) ?: return null
        return decode(bitmap) ?: decode(rotate(bitmap))
    }

    fun decode(bitmap: Bitmap): Pair<String, CodeFormat>? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        return tryDecode(source) ?: tryDecode(source.invert())
    }

    private fun tryDecode(source: LuminanceSource): Pair<String, CodeFormat>? {
        val reader = MultiFormatReader()
        val hints = ScannedCode.DECODE_HINTS + (DecodeHintType.TRY_HARDER to true)
        for (binarizer in listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))) {
            try {
                val result = reader.decode(BinaryBitmap(binarizer), hints)
                ScannedCode.fromResult(result)?.let { return it }
            } catch (e: ReaderException) {
                // try the next one
            } finally {
                reader.reset()
            }
        }
        return null
    }

    private fun load(context: Context, uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (e: Exception) {
        null
    }

    private fun rotate(bitmap: Bitmap): Bitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, false)
}
