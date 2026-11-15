package io.github.petrf22.vernostnikarta.barcode

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer

/**
 * Looks for a code in camera frames, fully offline (ZXing).
 * [onResult] is called once, on the analyzer thread.
 */
class BarcodeAnalyzer(private val onResult: (String, CodeFormat) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(ScannedCode.DECODE_HINTS)
    }
    private var done = false
    private var lastText: String? = null

    override fun analyze(image: ImageProxy) {
        try {
            if (done) return
            val width = image.width
            val height = image.height
            val luminance = copyLuminance(image, width, height)
            // 1D decoders only read along rows, so try the frame both ways.
            val result = decode(luminance, width, height)
                ?: decode(rotate90(luminance, width, height), height, width)
                ?: return
            val (text, format) = ScannedCode.fromResult(result) ?: return
            // 1D decoders occasionally misread a single frame; ask for the same value twice.
            if (!format.is2D && text != lastText) {
                lastText = text
                return
            }
            done = true
            onResult(text, format)
        } finally {
            image.close()
        }
    }

    private fun decode(data: ByteArray, width: Int, height: Int): Result? {
        val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        } catch (e: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun copyLuminance(image: ImageProxy, width: Int, height: Int): ByteArray {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val out = ByteArray(width * height)
        if (rowStride == width) {
            buffer.rewind()
            buffer.get(out, 0, minOf(out.size, buffer.remaining()))
        } else {
            for (row in 0 until height) {
                buffer.position(row * rowStride)
                buffer.get(out, row * width, minOf(width, buffer.remaining()))
            }
        }
        return out
    }

    private fun rotate90(data: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[x * height + (height - 1 - y)] = data[y * width + x]
            }
        }
        return out
    }
}
