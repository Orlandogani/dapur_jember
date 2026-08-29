package com.leanecorps.dapurjember.core.data.floor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.leanecorps.dapurjember.core.data.database.ChangeLogRecorder
import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase
import com.leanecorps.dapurjember.core.data.device.DeviceIdProvider
import com.leanecorps.dapurjember.core.domain.floor.DiningTable
import com.leanecorps.dapurjember.core.domain.floor.FloorArea
import com.leanecorps.dapurjember.core.domain.floor.TableState
import com.leanecorps.dapurjember.core.domain.floor.TableType
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FloorRepositoryImplTest {

    private lateinit var db: DapurJemberDatabase
    private lateinit var repo: FloorRepositoryImpl
    private val time = FakeTimeProvider(now = 1_000L)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DapurJemberDatabase::class.java)
            .allowMainThreadQueries().build()
        val deviceIds = DeviceIdProvider(context)
        repo = FloorRepositoryImpl(
            db = db,
            areaDao = db.floorAreaDao(),
            tableDao = db.diningTableDao(),
            changeLog = ChangeLogRecorder(db.changeLogDao(), deviceIds),
            time = time,
            deviceIds = deviceIds,
        )
    }

    @After
    fun tearDown() = db.close()

    private fun table(id: String) = DiningTable(
        id = id,
        floorAreaId = "hall",
        label = id,
        seats = 2,
        posX = 0.1,
        posY = 0.2,
        state = TableState.FREE,
        type = TableType.DINE_IN,
    )

    @Test
    fun `observeFloor nests tables under areas and logs each write`() = runTest {
        repo.upsertArea(FloorArea(id = "hall", name = "Hall"))
        repo.upsertTable(table("t1"))

        val floor = repo.observeFloor().first()
        assertEquals("Hall", floor.single().area.name)
        assertEquals(listOf("t1"), floor.single().tables.map { it.id })
        assertEquals(
            listOf("floor_area:INSERT", "dining_table:INSERT"),
            db.changeLogDao().observeUnsynced().first().map { "${it.entityType}:${it.op}" },
        )
    }

    @Test
    fun `setTableState changes the state and logs an UPDATE`() = runTest {
        repo.upsertArea(FloorArea(id = "hall", name = "Hall"))
        repo.upsertTable(table("t1"))
        time.advanceBy(1)

        repo.setTableState("t1", TableState.OCCUPIED)

        assertEquals(TableState.OCCUPIED, repo.getTable("t1")!!.state)
        val lastLog = db.changeLogDao().observeUnsynced().first().last()
        assertEquals("dining_table" to "UPDATE", lastLog.entityType to lastLog.op)
    }
}
