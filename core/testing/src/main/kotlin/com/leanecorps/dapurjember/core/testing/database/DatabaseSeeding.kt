package com.leanecorps.dapurjember.core.testing.database

import com.leanecorps.dapurjember.core.data.database.DapurJemberDatabase

/**
 * Inserts the menu / floor / staff / shift rows that the orders-cluster foreign keys need,
 * using the default ids from [MenuEntityFixtures] and [OperationalEntityFixtures].
 */
suspend fun DapurJemberDatabase.seedOrderPrerequisites() {
    categoryDao().upsert(MenuEntityFixtures.category())
    menuItemDao().upsert(MenuEntityFixtures.menuItem())
    menuVariantDao().upsert(MenuEntityFixtures.menuVariant())
    modifierGroupDao().upsert(MenuEntityFixtures.modifierGroup())
    modifierDao().upsert(MenuEntityFixtures.modifier())

    floorAreaDao().upsert(OperationalEntityFixtures.floorArea())
    diningTableDao().upsert(OperationalEntityFixtures.diningTable())
    staffDao().upsert(OperationalEntityFixtures.staff())
    shiftDao().upsert(OperationalEntityFixtures.shift())
}
