package io.github.petrf22.vernostnikarta.backup

import io.github.petrf22.vernostnikarta.data.CardRepository
import io.github.petrf22.vernostnikarta.data.SettingsStore
import kotlinx.coroutines.flow.first

class BackupManager(
    private val repository: CardRepository,
    private val settings: SettingsStore,
) {
    suspend fun exportJson(now: Long = System.currentTimeMillis()): String = BackupCodec.encode(
        BackupFile(
            format = BackupCodec.FORMAT,
            version = BackupCodec.VERSION,
            exportedAt = now,
            settings = BackupSettings(
                locationEnabled = settings.locationEnabled.first(),
                maxBrightness = settings.maxBrightness.first(),
            ),
            cards = repository.exportCards(),
        ),
    )

    /** Imports the cards and applies the settings from the file. @return number of imported cards */
    suspend fun import(backup: BackupFile, replace: Boolean): Int {
        val count = repository.importCards(backup.cards, replace)
        settings.setLocationEnabled(backup.settings.locationEnabled)
        settings.setMaxBrightness(backup.settings.maxBrightness)
        return count
    }
}
