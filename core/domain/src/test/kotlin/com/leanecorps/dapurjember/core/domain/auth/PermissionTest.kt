package com.leanecorps.dapurjember.core.domain.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PermissionTest {

    @Test
    fun `an owner can do everything`() {
        assertTrue(Permission.entries.all { StaffRole.OWNER.can(it) })
    }

    @Test
    fun `a manager can void and discount but cannot manage staff`() {
        assertTrue(StaffRole.MANAGER.can(Permission.VOID_SENT_LINE))
        assertTrue(StaffRole.MANAGER.can(Permission.APPLY_DISCOUNT))
        assertTrue(StaffRole.MANAGER.can(Permission.VIEW_REPORTS))
        assertFalse(StaffRole.MANAGER.can(Permission.MANAGE_STAFF))
    }

    @Test
    fun `a cashier closes shifts but cannot void a sent line or discount`() {
        assertTrue(StaffRole.CASHIER.can(Permission.CLOSE_SHIFT))
        assertFalse(StaffRole.CASHIER.can(Permission.VOID_SENT_LINE))
        assertFalse(StaffRole.CASHIER.can(Permission.APPLY_DISCOUNT))
    }

    @Test
    fun `a waiter holds no privileged permission`() {
        assertTrue(Permission.entries.none { StaffRole.WAITER.can(it) })
    }
}
