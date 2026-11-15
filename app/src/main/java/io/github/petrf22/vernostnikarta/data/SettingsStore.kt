package io.github.petrf22.vernostnikarta.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    /** Remember where cards are used (needs the location permission as well). */
    val locationEnabled: Flow<Boolean> = context.dataStore.data.map { it[LOCATION_ENABLED] ?: false }

    /** Turn the brightness up while a card is shown. */
    val maxBrightness: Flow<Boolean> = context.dataStore.data.map { it[MAX_BRIGHTNESS] ?: true }

    suspend fun setLocationEnabled(value: Boolean) {
        context.dataStore.edit { it[LOCATION_ENABLED] = value }
    }

    suspend fun setMaxBrightness(value: Boolean) {
        context.dataStore.edit { it[MAX_BRIGHTNESS] = value }
    }

    private companion object {
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val MAX_BRIGHTNESS = booleanPreferencesKey("max_brightness")
    }
}
