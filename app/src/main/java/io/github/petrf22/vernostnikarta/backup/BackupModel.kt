package io.github.petrf22.vernostnikarta.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The export file. Everything the app knows, in one human-readable JSON document.
 * [format] and [version] have no defaults on purpose: a JSON file without them is not ours.
 */
@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: Long = 0,
    val settings: BackupSettings = BackupSettings(),
    val cards: List<BackupCard> = emptyList(),
)

@Serializable
data class BackupSettings(
    val locationEnabled: Boolean = false,
    val maxBrightness: Boolean = true,
)

@Serializable
data class BackupCard(
    val uuid: String = "",
    val name: String,
    val note: String = "",
    val code: String,
    val barcodeFormat: String,
    /** "#RRGGBB" */
    val color: String = "",
    val position: Int = 0,
    val createdAt: Long = 0,
    val usages: List<BackupUsage> = emptyList(),
)

@Serializable
data class BackupUsage(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
)

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

object BackupCodec {
    const val FORMAT = "vernostni-karta"
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /** @throws BackupException when the text is not an export of this app or is from a newer version */
    fun decode(text: String): BackupFile {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException("Not a valid backup file", e)
        } catch (e: IllegalArgumentException) {
            throw BackupException("Not a valid backup file", e)
        }
        if (backup.format != FORMAT) throw BackupException("Unknown format: ${backup.format}")
        if (backup.version !in 1..VERSION) throw BackupException("Unsupported version: ${backup.version}")
        return backup
    }

    fun colorToHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    /** @return opaque ARGB, or null when [hex] is not "#RRGGBB" */
    fun hexToColor(hex: String): Int? {
        val digits = hex.removePrefix("#")
        if (digits.length != 6) return null
        val rgb = digits.toIntOrNull(16) ?: return null
        return rgb or (0xFF shl 24)
    }
}
