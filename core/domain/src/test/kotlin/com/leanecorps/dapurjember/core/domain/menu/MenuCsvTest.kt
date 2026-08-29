package com.leanecorps.dapurjember.core.domain.menu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuCsvTest {

    @Test
    fun `parses rows, skips the header, defaults a blank variant to Regular`() {
        val csv = """
            category,item,variant,price
            Rice,Nasi Goreng Ayam,Regular,25000
            Rice,Nasi Goreng Ayam,Large,30000
            Drinks,Es Teh,,5000
        """.trimIndent().trim()

        val result = MenuCsv.parse(csv, minorUnits = 0)

        assertTrue(result.errors.isEmpty())
        assertEquals(3, result.rows.size)
        assertEquals(listOf("Rice", "Drinks"), result.categories)
        assertEquals(2, result.itemCount)
        assertEquals("Regular", result.rows.last().variant)
        assertEquals(25_000L, result.rows.first().priceMinor)
    }

    @Test
    fun `converts major-unit prices with the currency scale`() {
        val result = MenuCsv.parse("Coffee,Latte,Regular,3.50", minorUnits = 2)
        assertEquals(350L, result.rows.single().priceMinor)
    }

    @Test
    fun `reports the line number for malformed rows and keeps the good ones`() {
        val csv = """
            Rice,Nasi Goreng,Regular,25000
            Rice,Broken Row,20000
            Rice,Bad Price,Regular,abc
            ,No Category,Regular,1000
        """.trimIndent().trim()

        val result = MenuCsv.parse(csv, minorUnits = 0)

        assertEquals(1, result.rows.size)
        assertEquals(listOf(2, 3, 4), result.errors.map { it.line })
    }

    @Test
    fun `an availability column of no marks the variant unavailable`() {
        val result = MenuCsv.parse("Rice,Nasi,Regular,10000,no", minorUnits = 0)
        assertEquals(false, result.rows.single().available)
    }

    @Test
    fun `empty input is not usable`() {
        assertTrue(!MenuCsv.parse("\n  \n", minorUnits = 0).isUsable)
    }
}
