package io.github.petrf22.vernostnikarta.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.petrf22.vernostnikarta.util.normalizeForSearch

const val DEFAULT_CARD_COLOR: Int = 0xFF1565C0.toInt()

/** Colors offered when editing a card (ARGB). */
val CARD_PALETTE: List<Int> = listOf(
    0xFFD32F2F, 0xFFE64A19, 0xFFF9A825, 0xFFFFEB3B, 0xFF7CB342, 0xFF2E7D32, 0xFF00897B,
    0xFF0288D1, 0xFF1565C0, 0xFF283593, 0xFF6A1B9A, 0xFFC2185B, 0xFF5D4037, 0xFF455A64,
    0xFF212121, 0xFFFAFAFA,
).map { it.toInt() }

/** Black or white, whichever reads better on [background]. */
fun contentColorOn(background: Color): Color = if (background.luminance() > 0.45f) Color(0xFF1B1B1B) else Color.White

/**
 * Approximate brand colors of common stores, so a new card looks roughly like the real one.
 * Colors only — no logos. Keys are lower-case words without diacritics.
 */
object StoreColors {
    private val colors: Map<String, Long> = mapOf(
        "albert" to 0xFF00A3E0,
        "billa" to 0xFFFFE600,
        "tesco" to 0xFF00539F,
        "lidl" to 0xFF0050AA,
        "kaufland" to 0xFFE10915,
        "penny" to 0xFFCD1414,
        "globus" to 0xFFF39200,
        "makro" to 0xFF003DA5,
        "coop" to 0xFFE30613,
        "zabka" to 0xFF008C45,
        "biedronka" to 0xFFE30613,
        "auchan" to 0xFFE0001A,
        "carrefour" to 0xFF004E9F,
        "spar" to 0xFF007A3E,
        "interspar" to 0xFF007A3E,
        "aldi" to 0xFF00005F,
        "rewe" to 0xFFCC071E,
        "edeka" to 0xFFFFE500,
        "dm" to 0xFF1F3685,
        "rossmann" to 0xFFC3002D,
        "teta" to 0xFFE2007A,
        "muller" to 0xFFF16E22,
        "dr max" to 0xFF6CB52D,
        "drmax" to 0xFF6CB52D,
        "benu" to 0xFF009B77,
        "ikea" to 0xFF0058A3,
        "jysk" to 0xFF143C8A,
        "decathlon" to 0xFF0082C3,
        "sportisimo" to 0xFFE2001A,
        "h m" to 0xFFE50010,
        "deichmann" to 0xFF00843D,
        "ccc" to 0xFFF26F21,
        "pepco" to 0xFF0057B8,
        "action" to 0xFF0071CE,
        "obi" to 0xFFFF7E21,
        "hornbach" to 0xFFF7911A,
        "bauhaus" to 0xFFE30613,
        "mountfield" to 0xFF009640,
        "mediamarkt" to 0xFFDF0000,
        "media markt" to 0xFFDF0000,
        "sephora" to 0xFF111111,
        "notino" to 0xFF111111,
        "yves rocher" to 0xFF00693C,
        "shell" to 0xFFFBCE07,
        "orlen" to 0xFFE30613,
        "omv" to 0xFF003366,
    )

    /** @return ARGB of the first known store whose name appears in [storeName] as whole words, or null */
    fun suggest(storeName: String): Int? {
        val words = normalizeForSearch(storeName).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val haystack = " " + words.joinToString(" ") + " "
        return colors.entries.firstOrNull { haystack.contains(" " + it.key + " ") }?.value?.toInt()
    }
}
