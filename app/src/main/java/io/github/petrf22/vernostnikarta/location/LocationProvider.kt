package io.github.petrf22.vernostnikarta.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Device position from the platform [LocationManager] — deliberately not Google Play Services.
 * The position is only ever used inside the app.
 */
class LocationProvider(private val context: Context) {
    private val manager: LocationManager? = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean = PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** @return the current position, or null when it is not available (no permission, location off, timeout) */
    @SuppressLint("MissingPermission")
    suspend fun current(): GeoPoint? {
        val manager = manager ?: return null
        if (!hasPermission()) return null
        return try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) return null
            val providers = manager.getProviders(true)
            val now = System.currentTimeMillis()
            val last = providers
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            if (last != null && now - last.time < FRESH_MILLIS) return last.toPoint()

            val provider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in providers ->
                    LocationManager.FUSED_PROVIDER
                LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
                LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
                else -> null
            }
            val fresh = provider?.let { withTimeoutOrNull(TIMEOUT_MILLIS) { request(manager, it) } }
            (fresh ?: last?.takeIf { now - it.time < USABLE_MILLIS })?.toPoint()
        } catch (e: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun request(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                ) { location: Location? ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private fun Location.toPoint() = GeoPoint(latitude, longitude)

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private const val FRESH_MILLIS = 2 * 60_000L
        private const val USABLE_MILLIS = 15 * 60_000L
        private const val TIMEOUT_MILLIS = 8_000L
    }
}
