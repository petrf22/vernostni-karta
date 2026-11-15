package io.github.petrf22.vernostnikarta

import io.github.petrf22.vernostnikarta.data.UsageEntity
import io.github.petrf22.vernostnikarta.location.GeoPoint
import io.github.petrf22.vernostnikarta.location.NearbyRanker
import org.junit.Assert.assertEquals
import org.junit.Test

class NearbyRankerTest {
    private val day = 86_400_000L
    private val now = 1_700_000_000_000L
    private val brno = GeoPoint(49.1951, 16.6068)

    private fun usage(cardId: Long, lat: Double, lon: Double, ageDays: Long = 0) =
        UsageEntity(cardId = cardId, latitude = lat, longitude = lon, timestamp = now - ageDays * day)

    @Test
    fun distance() {
        // Brno – Prague is roughly 185 km.
        val d = NearbyRanker.distanceMeters(49.1951, 16.6068, 50.0755, 14.4378)
        assertEquals(185_000.0, d, 3_000.0)
        assertEquals(0.0, NearbyRanker.distanceMeters(49.0, 16.0, 49.0, 16.0), 0.001)
    }

    @Test
    fun onlyCardsUsedWithinTheRadius() {
        val usages = listOf(
            usage(1, 49.1951, 16.6068),
            usage(2, 49.1960, 16.6068), // ~100 m north
            usage(3, 49.2000, 16.6068), // ~545 m north
            usage(4, 50.0755, 14.4378),
        )
        assertEquals(setOf(1L, 2L), NearbyRanker.rank(usages, brno, now).toSet())
    }

    @Test
    fun moreUsesWin() {
        val usages = listOf(
            usage(1, 49.1951, 16.6068),
            usage(2, 49.1951, 16.6068),
            usage(2, 49.1952, 16.6069),
        )
        assertEquals(listOf(2L, 1L), NearbyRanker.rank(usages, brno, now))
    }

    @Test
    fun recentUseBeatsOldOne() {
        val usages = listOf(
            usage(1, 49.1951, 16.6068, ageDays = 400),
            usage(2, 49.1951, 16.6068, ageDays = 1),
        )
        assertEquals(listOf(2L, 1L), NearbyRanker.rank(usages, brno, now))
    }

    @Test
    fun nothingNearby() {
        assertEquals(emptyList<Long>(), NearbyRanker.rank(emptyList(), brno, now))
        assertEquals(emptyList<Long>(), NearbyRanker.rank(listOf(usage(1, 50.0, 14.0)), brno, now))
    }
}
