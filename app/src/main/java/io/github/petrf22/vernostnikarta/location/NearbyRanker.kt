package io.github.petrf22.vernostnikarta.location

import io.github.petrf22.vernostnikarta.data.UsageEntity
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

/** Decides which cards to offer at the current place. Pure logic, no Android. */
object NearbyRanker {
    const val DEFAULT_RADIUS_METERS = 200.0
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val DAY_MILLIS = 86_400_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * @return ids of cards used within [radiusMeters] of [here], best match first.
     *   Every use counts; recent ones count more (a month-old use weighs half).
     */
    fun rank(
        usages: List<UsageEntity>,
        here: GeoPoint,
        now: Long,
        radiusMeters: Double = DEFAULT_RADIUS_METERS,
    ): List<Long> {
        val scores = LinkedHashMap<Long, Double>()
        for (usage in usages) {
            val distance = distanceMeters(here.latitude, here.longitude, usage.latitude, usage.longitude)
            if (distance > radiusMeters) continue
            val ageDays = (now - usage.timestamp).coerceAtLeast(0) / DAY_MILLIS
            scores[usage.cardId] = (scores[usage.cardId] ?: 0.0) + 1.0 / (1.0 + ageDays / 30.0)
        }
        return scores.entries.sortedByDescending { it.value }.map { it.key }
    }
}
