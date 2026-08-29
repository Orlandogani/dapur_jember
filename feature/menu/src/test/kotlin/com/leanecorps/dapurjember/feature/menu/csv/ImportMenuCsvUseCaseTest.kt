package com.leanecorps.dapurjember.feature.menu.csv

import com.leanecorps.dapurjember.core.domain.config.StoreProfile
import com.leanecorps.dapurjember.core.domain.menu.Category
import com.leanecorps.dapurjember.core.domain.menu.ImportMenuCsvUseCase
import com.leanecorps.dapurjember.core.domain.pricing.RoundingRule
import com.leanecorps.dapurjember.core.testing.repository.FakeMenuRepository
import com.leanecorps.dapurjember.core.testing.repository.FakeStoreProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportMenuCsvUseCaseTest {

    private val menu = FakeMenuRepository()
    private val profiles = FakeStoreProfileRepository(
        StoreProfile(
            id = "p1",
            name = "Test",
            currencyCode = "IDR",
            currencyMinorUnits = 0,
            taxRateBasisPoints = 0,
            taxInclusive = false,
            serviceChargeBasisPoints = 0,
            serviceChargeTaxable = false,
            roundingRule = RoundingRule.NONE,
            businessDayCutoffMinutes = 0,
            timezoneId = "UTC",
        ),
    )
    private val useCase = ImportMenuCsvUseCase(menu, profiles)

    @Test
    fun `imports categories and items, reusing an existing category by name`() = runTest {
        menu.upsertCategory(Category(id = "c-existing", name = "Rice"))

        val summary = useCase(
            """
            category,item,variant,price
            Rice,Nasi Goreng Ayam,Regular,25000
            Rice,Nasi Goreng Ayam,Large,30000
            Drinks,Es Teh,,5000
            """.trimIndent().trim(),
        )

        assertEquals(1, summary.categoriesAdded) // only "Drinks" is new
        assertEquals(2, summary.itemsImported)

        val riceItems = menu.observeItems("c-existing").first()
        assertEquals(listOf("Nasi Goreng Ayam"), riceItems.map { it.name })
        val variants = menu.observeItemWithVariants(riceItems.single().id).first()!!.variants
        assertEquals(listOf(25_000L, 30_000L), variants.sortedBy { it.sortOrder }.map { it.price.minor })
    }

    @Test
    fun `a file with only bad rows imports nothing and returns the errors`() = runTest {
        val summary = useCase("Rice,Broken\nRice,Bad,Regular,abc")

        assertEquals(0, summary.itemsImported)
        assertEquals(0, summary.categoriesAdded)
        assertEquals(listOf(1, 2), summary.errors.map { it.line })
    }
}
