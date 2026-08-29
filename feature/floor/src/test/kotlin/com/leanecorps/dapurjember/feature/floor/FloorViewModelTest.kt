package com.leanecorps.dapurjember.feature.floor

import app.cash.turbine.test
import com.leanecorps.dapurjember.core.domain.floor.DiningTable
import com.leanecorps.dapurjember.core.domain.floor.FloorArea
import com.leanecorps.dapurjember.core.domain.floor.TableState
import com.leanecorps.dapurjember.core.domain.floor.TableType
import com.leanecorps.dapurjember.core.testing.coroutines.MainDispatcherExtension
import com.leanecorps.dapurjember.core.testing.repository.FakeFloorRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class FloorViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeFloorRepository()
    private fun viewModel() = FloorViewModel(repository)

    private fun table(id: String, areaId: String, state: TableState = TableState.FREE) = DiningTable(
        id = id,
        floorAreaId = areaId,
        label = id.uppercase(),
        seats = 4,
        posX = 0.0,
        posY = 0.0,
        state = state,
        type = TableType.DINE_IN,
    )

    @Test
    fun `uiState groups tables under their area`() = runTest {
        repository.upsertArea(FloorArea(id = "hall", name = "Main Hall"))
        repository.upsertTable(table("t1", "hall"))
        repository.upsertTable(table("t2", "hall", TableState.OCCUPIED))

        viewModel().uiState.test {
            val loaded = expectMostRecentItem()
            assertEquals(listOf("Main Hall"), loaded.areas.map { it.name })
            assertEquals(
                listOf(TableState.FREE, TableState.OCCUPIED),
                loaded.areas.single().tables.map { it.state },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `markTableState updates the table`() = runTest {
        repository.upsertArea(FloorArea(id = "hall", name = "Hall"))
        repository.upsertTable(table("t1", "hall"))
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(TableState.FREE, expectMostRecentItem().areas.single().tables.single().state)
            vm.markTableState("t1", TableState.NEEDS_CLEANING)
            assertEquals(
                TableState.NEEDS_CLEANING,
                awaitItem().areas.single().tables.single().state,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
