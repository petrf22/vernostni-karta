package io.github.petrf22.vernostnikarta

import android.app.Application
import android.content.Context
import io.github.petrf22.vernostnikarta.backup.BackupManager
import io.github.petrf22.vernostnikarta.data.AppDatabase
import io.github.petrf22.vernostnikarta.data.CardRepository
import io.github.petrf22.vernostnikarta.data.SettingsStore
import io.github.petrf22.vernostnikarta.location.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Hand-made dependency injection; the app is small. */
class AppContainer(context: Context) {
    /** Outlives screens — for work that must finish after the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val repository = CardRepository(AppDatabase.build(context))
    val settings = SettingsStore(context)
    val location = LocationProvider(context)
    val backup = BackupManager(repository, settings)

    /** Remembers that the card was shown here — only if the user turned this on and location is available. */
    fun recordUsage(cardId: Long) {
        appScope.launch {
            if (!settings.locationEnabled.first()) return@launch
            val here = location.current() ?: return@launch
            repository.addUsage(cardId, here.latitude, here.longitude, System.currentTimeMillis())
        }
    }
}
