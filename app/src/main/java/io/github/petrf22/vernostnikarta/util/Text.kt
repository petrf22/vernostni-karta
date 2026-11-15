package io.github.petrf22.vernostnikarta.util

import java.text.Normalizer
import java.util.Locale

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/**
 * Lower-cases the text and strips diacritics, so that "zabka" matches "Žabka".
 * Letters that do not decompose in Unicode (ł, ß, ø, đ) are mapped by hand.
 */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace('ł', 'l')
        .replace('ø', 'o')
        .replace('đ', 'd')
        .replace("ß", "ss")
        .trim()
