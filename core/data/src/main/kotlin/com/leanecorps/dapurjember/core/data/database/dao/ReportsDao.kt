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

    @Query(
        "SELECT l.item_name_snapshot AS name, SUM(l.qty) AS quantity, " +
            "SUM(l.qty * l.unit_price_snapshot_minor) AS grossMinor " +
            "FROM order_line l INNER JOIN orders o ON o.id = l.order_id " +
            "WHERE o.business_day = :businessDay AND o.state IN ('PAID', 'CLOSED') " +
            "AND l.state = 'ACTIVE' AND l.deleted_at IS NULL AND o.deleted_at IS NULL " +
            "GROUP BY l.item_name_snapshot ORDER BY grossMinor DESC",
    )
    suspend fun salesByItem(businessDay: String): List<ItemSalesRow>
}

data class DailyTotalsRow(val orderCount: Int, val revenueMinor: Long, val covers: Int)

data class PaymentMixRow(val method: String, val amountMinor: Long)

data class CountAndTotalRow(val count: Int, val totalMinor: Long)

data class ItemSalesRow(val name: String, val quantity: Int, val grossMinor: Long)
