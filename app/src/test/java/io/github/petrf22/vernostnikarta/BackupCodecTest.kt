package io.github.petrf22.vernostnikarta

import io.github.petrf22.vernostnikarta.backup.BackupCard
import io.github.petrf22.vernostnikarta.backup.BackupCodec
import io.github.petrf22.vernostnikarta.backup.BackupException
import io.github.petrf22.vernostnikarta.backup.BackupFile
import io.github.petrf22.vernostnikarta.backup.BackupSettings
import io.github.petrf22.vernostnikarta.backup.BackupUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    private val sample = BackupFile(
        format = BackupCodec.FORMAT,
        version = BackupCodec.VERSION,
        exportedAt = 1_700_000_000_000,
        settings = BackupSettings(locationEnabled = true, maxBrightness = false),
        cards = listOf(
            BackupCard(
                uuid = "a-1",
                name = "Žabka „u nádraží“",
                note = "rodinná",
                code = "2401234567890",
                barcodeFormat = "EAN_13",
                color = "#008C45",
                position = 3,
                createdAt = 42,
                usages = listOf(BackupUsage(49.1951, 16.6068, 1_699_000_000_000)),
            ),
        ),
    )

    @Test
    fun roundTripKeepsEverything() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun exportIsMarkedWithFormatAndVersion() {
        val text = BackupCodec.encode(sample)
        assert(text.contains("\"format\": \"vernostni-karta\"")) { text }
        assert(text.contains("\"version\": ${BackupCodec.VERSION}")) { text }
    }

    @Test
    fun unknownFieldsAreIgnoredAndMissingOnesGetDefaults() {
        val text = """
            {"format":"vernostni-karta","version":1,"somethingNew":true,
             "cards":[{"name":"Lidl","code":"123","barcodeFormat":"CODE_128","future":1}]}
        """.trimIndent()
        val backup = BackupCodec.decode(text)
        assertEquals(1, backup.cards.size)
        assertEquals("Lidl", backup.cards[0].name)
        assertEquals("", backup.cards[0].note)
        assertEquals(BackupSettings(), backup.settings)
    }

    @Test
    fun foreignOrBrokenFilesAreRejected() {
        assertThrows(BackupException::class.java) { BackupCodec.decode("not json") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("""{"format":"other","version":1}""") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("""{"cards":[{"name":"x"}]}""") }
        // Any other JSON document must not pass for an empty backup.
        assertThrows(BackupException::class.java) { BackupCodec.decode("""{"hello":1}""") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("{}") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("""{"format":"vernostni-karta"}""") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("[]") }
    }

    @Test
    fun newerVersionIsRejected() {
        assertThrows(BackupException::class.java) {
            BackupCodec.decode("""{"format":"vernostni-karta","version":${BackupCodec.VERSION + 1}}""")
        }
    }

    @Test
    fun colors() {
        assertEquals("#008C45", BackupCodec.colorToHex(0xFF008C45.toInt()))
        assertEquals(0xFF008C45.toInt(), BackupCodec.hexToColor("#008c45"))
        assertEquals(0xFFFFFFFF.toInt(), BackupCodec.hexToColor("FFFFFF"))
        assertNull(BackupCodec.hexToColor(""))
        assertNull(BackupCodec.hexToColor("#12"))
        assertNull(BackupCodec.hexToColor("#GGGGGG"))
    }
}
