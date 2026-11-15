package io.github.petrf22.vernostnikarta.ui.list

import io.github.petrf22.vernostnikarta.data.CardEntity
import io.github.petrf22.vernostnikarta.util.normalizeForSearch

/**
 * Quick search in the list. Ignores case and diacritics, matches anywhere in the store name
 * (and in the note). Best matches first: name starts with the query, then a word of the name
 * starts with it, then anywhere in the name, then the note. Within a group the list order is kept.
 */
fun filterCards(cards: List<CardEntity>, query: String): List<CardEntity> {
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return cards
    return cards
        .mapNotNull { card ->
            val name = normalizeForSearch(card.name)
            val rank = when {
                name.startsWith(q) -> 0
                name.split(' ', '-', '.', '&', '/').any { it.startsWith(q) } -> 1
                name.contains(q) -> 2
                normalizeForSearch(card.note).contains(q) -> 3
                else -> return@mapNotNull null
            }
            rank to card
        }
        .sortedBy { it.first }
        .map { it.second }
}
