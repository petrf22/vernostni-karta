package io.github.petrf22.vernostnikarta

import io.github.petrf22.vernostnikarta.data.CardEntity
import io.github.petrf22.vernostnikarta.ui.list.filterCards
import org.junit.Assert.assertEquals
import org.junit.Test

class CardFilterTest {
    private fun card(id: Long, name: String, note: String = "") =
        CardEntity(id = id, uuid = "u$id", name = name, note = note, code = "1", format = "QR_CODE", color = 0)

    private val cards = listOf(
        card(1, "Albert"),
        card(2, "Žabka"),
        card(3, "Dr. Max", note = "lékárna"),
        card(4, "Billa"),
        card(5, "Globus", note = "Albertov"),
        card(6, "Müller"),
        card(7, "Biedronka Łódź"),
    )

    private fun names(query: String) = filterCards(cards, query).map { it.name }

    @Test
    fun emptyQueryKeepsTheListAsIs() {
        assertEquals(cards, filterCards(cards, ""))
        assertEquals(cards, filterCards(cards, "   "))
    }

    @Test
    fun ignoresCaseAndDiacritics() {
        assertEquals(listOf("Žabka"), names("zabka"))
        assertEquals(listOf("Žabka"), names("ŽAB"))
        assertEquals(listOf("Müller"), names("mull"))
        assertEquals(listOf("Biedronka Łódź"), names("lodz"))
    }

    @Test
    fun matchesAnywhereInTheName() {
        assertEquals(listOf("Albert", "Globus"), names("bert"))
    }

    @Test
    fun startOfNameComesFirstThenWordStartThenNote() {
        // "b": Billa and Biedronka start with it; nothing else has a word starting with b;
        // Albert, Žabka and Globus merely contain it.
        assertEquals(listOf("Billa", "Biedronka Łódź", "Albert", "Žabka", "Globus"), names("b"))
        assertEquals(listOf("Dr. Max"), names("max"))
        assertEquals(listOf("Dr. Max"), names("lekarna"))
    }

    @Test
    fun noMatch() {
        assertEquals(emptyList<String>(), names("tesco"))
    }
}
