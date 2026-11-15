package io.github.petrf22.vernostnikarta

import io.github.petrf22.vernostnikarta.ui.StoreColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StoreColorsTest {
    @Test
    fun knownStores() {
        assertNotNull(StoreColors.suggest("Lidl"))
        assertEquals(StoreColors.suggest("Lidl"), StoreColors.suggest("  LIDL Plus "))
        assertEquals(StoreColors.suggest("zabka"), StoreColors.suggest("Žabka"))
        assertEquals(StoreColors.suggest("drmax"), StoreColors.suggest("Dr.Max lékárna"))
        assertEquals(StoreColors.suggest("muller"), StoreColors.suggest("Müller"))
        assertNotNull(StoreColors.suggest("H&M"))
        assertNotNull(StoreColors.suggest("dm drogerie"))
    }

    @Test
    fun onlyWholeWordsMatch() {
        assertNull(StoreColors.suggest("Admiral"))
        assertNull(StoreColors.suggest("Sparta"))
        assertNull(StoreColors.suggest("Hobby"))
    }

    @Test
    fun unknownOrEmpty() {
        assertNull(StoreColors.suggest(""))
        assertNull(StoreColors.suggest("Pekařství U Nováků"))
    }

    @Test
    fun colorsAreOpaque() {
        assertEquals(0xFF, StoreColors.suggest("Tesco")!! ushr 24)
    }
}
