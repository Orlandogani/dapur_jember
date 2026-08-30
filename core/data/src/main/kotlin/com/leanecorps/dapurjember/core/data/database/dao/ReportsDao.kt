package com.leanecorps.dapurjember.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Read-only aggregate queries for the reports feature. Every query keys on the indexed
 * `orders.business_day` TEXT column.
 */
@Dao
interface ReportsDao {

    @Query(
        "SELECT COUNT(*) AS orderCount, COALESCE(SUM(total_minor), 0) AS revenueMinor, " +
            "COALESCE(SUM(guest_count), 0) AS covers FROM orders " +
            "WHERE business_day = :businessDay AND state IN ('PAID', 'CLOSED') AND deleted_at IS NULL",
    )
    suspend fun dailyTotals(businessDay: String): DailyTotalsRow

    @Query(
        "SELECT p.method AS method, COALESCE(SUM(p.amount_minor), 0) AS amountMinor FROM payment p " +
            "INNER JOIN orders o ON o.id = p.order_id " +
            "WHERE o.business_day = :businessDay AND o.deleted_at IS NULL AND p.deleted_at IS NULL " +
            "GROUP BY p.method ORDER BY amountMinor DESC",
    )
    suspend fun paymentMix(businessDay: String): List<PaymentMixRow>

    @Query(
        "SELECT COUNT(*) AS count, COALESCE(SUM(d.computed_minor), 0) AS totalMinor FROM discount d " +
            "INNER JOIN orders o ON o.id = d.order_id " +
            "WHERE o.business_day = :businessDay AND o.deleted_at IS NULL AND d.deleted_at IS NULL",
    )
    suspend fun discounts(businessDay: String): CountAndTotalRow

    @Query(
        "SELECT COUNT(*) FROM orders WHERE business_day = :businessDay AND state = 'VOIDED' AND deleted_at IS NULL",
    )
    suspend fun voidedOrderCount(businessDay: String): Int

    @Query(
        "SELECT COUNT(*) FROM order_line l INNER JOIN orders o ON o.id = l.order_id " +
            "WHERE o.business_day = :businessDay AND l.state = 'VOIDED' " +
            "AND l.deleted_at IS NULL AND o.deleted_at IS NULL",
    )
    suspend fun voidedLineCount(businessDay: String): Int

    /**
     * Cost of goods sold for the day (FR-I4). Read straight off the SALE `stock_movement`
     * rows the payment transaction wrote — each carries the ingredient's average cost *at the
     * moment of sale*, so re-costing an ingredient later never rewrites history.
     * `qty_base_delta` is negative for a sale, hence the leading minus.
     */
    @Query(
        "SELECT COALESCE(CAST(ROUND(SUM(-m.qty_base_delta * m.unit_cost_minor)) AS INTEGER), 0) " +
            "FROM stock_movement m " +
            "INNER JOIN order_line l ON l.id = m.order_line_id " +
            "INNER JOIN orders o ON o.id = l.order_id " +
            "WHERE o.business_day = :businessDay AND m.reason = 'SALE' " +
            "AND m.deleted_at IS NULL AND l.deleted_at IS NULL AND o.deleted_at IS NULL",
    )
    suspend fun cogsMinor(businessDay: String): Long

    @Query(
        "SELECT l.item_name_snapshot AS name, SUM(l.qty) AS quantity, " +
            "SUM(l.qty * l.unit_price_snapshot_minor) AS grossMinor, " +
            "COALESCE(CAST(ROUND(SUM(" +
            "  (SELECT COALESCE(SUM(-m.qty_base_delta * m.unit_cost_minor), 0) " +
            "   FROM stock_movement m WHERE m.order_line_id = l.id " +
            "   AND m.reason = 'SALE' AND m.deleted_at IS NULL)" +
            ")) AS INTEGER), 0) AS costMinor " +
            "FROM order_line l INNER JOIN orders o ON o.id = l.order_id " +
            "WHERE o.business_day = :businessDay AND o.state IN ('PAID', 'CLOSED') " +
            "AND l.state = 'ACTIVE' AND l.deleted_at IS NULL AND o.deleted_at IS NULL " +
            "GROUP BY l.item_name_snapshot ORDER BY grossMinor DESC",
    )
    suspend fun salesByItem(businessDay: String): List<ItemSalesRow>

    /**
     * Sales grouped by category. Unlike the item name, the category is not snapshotted on the
     * order line, so this joins through to the live `category` row — renaming a category
     * retroactively regroups history, which is the behaviour an owner expects here.
     */
    @Query(
        "SELECT c.name AS name, SUM(l.qty) AS quantity, " +
            "SUM(l.qty * l.unit_price_snapshot_minor) AS grossMinor " +
            "FROM order_line l " +
            "INNER JOIN orders o ON o.id = l.order_id " +
            "INNER JOIN menu_variant v ON v.id = l.menu_variant_id " +
            "INNER JOIN menu_item i ON i.id = v.menu_item_id " +
            "INNER JOIN category c ON c.id = i.category_id " +
            "WHERE o.business_day = :businessDay AND o.state IN ('PAID', 'CLOSED') " +
            "AND l.state = 'ACTIVE' AND l.deleted_at IS NULL AND o.deleted_at IS NULL " +
            "GROUP BY c.name ORDER BY grossMinor DESC",
    )
    suspend fun salesByCategory(businessDay: String): List<CategorySalesRow>

    /** Voided lines for the day, with the staff who added the line and the stated reason (S25). */
    @Query(
        "SELECT s.name AS staffName, l.item_name_snapshot AS description, l.void_reason AS reason, " +
            "(l.qty * l.unit_price_snapshot_minor) AS amountMinor, l.updated_at AS at " +
            "FROM order_line l " +
            "INNER JOIN orders o ON o.id = l.order_id " +
            "INNER JOIN staff s ON s.id = l.added_by_staff_id " +
            "WHERE o.business_day = :businessDay AND l.state = 'VOIDED' " +
            "AND l.deleted_at IS NULL AND o.deleted_at IS NULL ORDER BY l.updated_at DESC",
    )
    suspend fun voidedLines(businessDay: String): List<AuditRow>

    /** Discounts applied on the day, with who authorised each (S25). */
    @Query(
        "SELECT s.name AS staffName, d.type AS description, d.reason AS reason, " +
            "d.computed_minor AS amountMinor, d.created_at AS at " +
            "FROM discount d " +
            "INNER JOIN orders o ON o.id = d.order_id " +
            "INNER JOIN staff s ON s.id = d.authorised_by_staff_id " +
            "WHERE o.business_day = :businessDay AND d.deleted_at IS NULL AND o.deleted_at IS NULL " +
            "ORDER BY d.created_at DESC",
    )
    suspend fun discountEntries(businessDay: String): List<AuditRow>
}

data class DailyTotalsRow(val orderCount: Int, val revenueMinor: Long, val covers: Int)

data class PaymentMixRow(val method: String, val amountMinor: Long)

data class CountAndTotalRow(val count: Int, val totalMinor: Long)

data class ItemSalesRow(val name: String, val quantity: Int, val grossMinor: Long, val costMinor: Long)

data class CategorySalesRow(val name: String, val quantity: Int, val grossMinor: Long)

data class AuditRow(
    val staffName: String,
    val description: String,
    val reason: String?,
    val amountMinor: Long,
    val at: Long,
)
