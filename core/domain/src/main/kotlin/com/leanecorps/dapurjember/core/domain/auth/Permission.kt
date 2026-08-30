package com.leanecorps.dapurjember.core.domain.auth

/**
 * The privileged actions a role may hold (FR-A2). Checked in use cases, never only in a
 * Composable — UI-only gating is trivially bypassed by an interposed navigation (arch §7).
 */
enum class Permission {
    VOID_SENT_LINE,
    APPLY_DISCOUNT,
    VIEW_REPORTS,
    ADJUST_STOCK,
    MANAGE_MENU,
    MANAGE_STAFF,
    CLOSE_SHIFT,
}

/**
 * The default permission matrix. `permissions_json` on `staff` can override this per person
 * later; until then a role's defaults are the whole story.
 */
fun StaffRole.can(permission: Permission): Boolean = when (this) {
    StaffRole.OWNER -> true
    StaffRole.MANAGER -> permission != Permission.MANAGE_STAFF
    StaffRole.CASHIER -> permission in CASHIER_PERMISSIONS
    StaffRole.WAITER -> false
}

private val CASHIER_PERMISSIONS = setOf(Permission.CLOSE_SHIFT)
