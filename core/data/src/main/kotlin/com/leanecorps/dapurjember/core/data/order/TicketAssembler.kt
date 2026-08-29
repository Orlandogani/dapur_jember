package com.leanecorps.dapurjember.core.data.order

import com.leanecorps.dapurjember.core.data.database.dao.DiningTableDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineDao
import com.leanecorps.dapurjember.core.data.database.dao.OrderLineModifierDao
import com.leanecorps.dapurjember.core.data.database.dao.PaymentDao
import com.leanecorps.dapurjember.core.data.database.dao.PrinterConfigDao
import com.leanecorps.dapurjember.core.data.database.dao.StaffDao
import com.leanecorps.dapurjember.core.data.database.dao.StoreProfileDao
import com.leanecorps.dapurjember.core.data.database.entity.OrderLineEntity
import com.leanecorps.dapurjember.core.data.database.entity.StoreProfileEntity
import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketData
import com.leanecorps.dapurjember.core.domain.printing.KitchenTicketLine
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import com.leanecorps.dapurjember.core.domain.printing.ReceiptData
import com.leanecorps.dapurjember.core.domain.printing.ReceiptItemLine
import com.leanecorps.dapurjember.core.domain.printing.ReceiptModifierLine
import com.leanecorps.dapurjember.core.domain.printing.ReceiptPaymentLine
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val DEFAULT_PAPER_MM = 80
private const val KITCHEN_STATION = "Kitchen"
private val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * Turns the persisted order graph into the print-ready [KitchenTicketData] / [ReceiptData]
 * the templates want. All ids are resolved to names here and the timestamp is formatted in
 * the store's timezone — the templates do no lookups.
 */
@Suppress("LongParameterList") // an assembler pulls from many tables by nature
internal class TicketAssembler @Inject constructor(
    private val orderDao: OrderDao,
    private val lineDao: OrderLineDao,
    private val lineModifierDao: OrderLineModifierDao,
    private val paymentDao: PaymentDao,
    private val staffDao: StaffDao,
    private val tableDao: DiningTableDao,
    private val storeProfileDao: StoreProfileDao,
    private val printerConfigDao: PrinterConfigDao,
) {

    suspend fun paperWidthMmFor(role: PrinterRole): Int =
        printerConfigDao.forRole(role.name).firstOrNull()?.paperWidthMm ?: DEFAULT_PAPER_MM

    suspend fun kitchenTicket(orderId: String, sentLines: List<OrderLineEntity>, now: Long): KitchenTicketData? {
        val order = orderDao.getById(orderId)?.takeIf { sentLines.isNotEmpty() } ?: return null
        val profile = storeProfileDao.get()
        val table = order.diningTableId?.let { tableDao.getById(it) }

        return KitchenTicketData(
            storeName = profile?.name.orEmpty(),
            stationTitle = KITCHEN_STATION,
            orderNumber = order.orderNumber,
            tableLabel = table?.label,
            orderType = table?.type?.let(::prettyType) ?: "Walk-up",
            serverName = staffDao.getById(order.openedByStaffId)?.name ?: "Staff",
            printedAt = formatTimestamp(now, profile?.timezoneId),
            lines = sentLines.map { line ->
                KitchenTicketLine(
                    quantity = line.qty,
                    name = displayName(line),
                    modifiers = lineModifierDao.getForLine(line.id).map { it.nameSnapshot },
                    note = line.lineNote,
                    course = line.course,
                )
            },
        )
    }

    suspend fun receipt(orderId: String, now: Long): ReceiptData? {
        val order = orderDao.getById(orderId) ?: return null
        val profile = storeProfileDao.get()
        val table = order.diningTableId?.let { tableDao.getById(it) }
        val payments = paymentDao.getForOrder(orderId)
        val activeLines = lineDao.getForOrder(orderId).filter { it.state == "ACTIVE" }

        return ReceiptData(
            headerLines = headerLines(profile),
            orderNumber = order.orderNumber,
            businessDay = order.businessDay,
            tableLabel = table?.label,
            printedAt = formatTimestamp(now, profile?.timezoneId),
            serverName = staffDao.getById(order.openedByStaffId)?.name ?: "Staff",
            lines = activeLines.map { line ->
                val mods = lineModifierDao.getForLine(line.id)
                val unit = line.unitPriceSnapshotMinor + mods.sumOf { it.priceDeltaSnapshotMinor }
                ReceiptItemLine(
                    quantity = line.qty,
                    name = displayName(line),
                    lineTotalMinor = unit * line.qty,
                    modifiers = mods.map { ReceiptModifierLine(it.nameSnapshot, it.priceDeltaSnapshotMinor) },
                )
            },
            subtotalMinor = order.subtotalMinor,
            discountMinor = order.discountMinor,
            serviceChargeMinor = order.serviceChargeMinor,
            taxMinor = order.taxMinor,
            roundingMinor = order.roundingMinor,
            totalMinor = order.totalMinor,
            payments = payments.map { ReceiptPaymentLine(it.method, it.amountMinor) },
            changeMinor = payments.sumOf { it.changeMinor },
            currencyCode = profile?.currencyCode ?: "",
            currencyMinorUnits = profile?.currencyMinorUnits ?: 0,
            footerLines = profile?.receiptFooter?.lines().orEmpty().filter { it.isNotBlank() },
        )
    }

    private fun headerLines(profile: StoreProfileEntity?): List<String> {
        if (profile == null) return emptyList()
        val custom = profile.receiptHeader?.takeIf { it.isNotBlank() }?.lines()?.filter { it.isNotBlank() }
        return custom
            ?: listOfNotNull(profile.name, profile.address, profile.phone, profile.taxId?.let { "NPWP $it" })
    }

    private fun displayName(line: OrderLineEntity): String =
        if (line.variantNameSnapshot.equals("Regular", ignoreCase = true)) {
            line.itemNameSnapshot
        } else {
            "${line.itemNameSnapshot} (${line.variantNameSnapshot})"
        }

    private fun prettyType(raw: String): String =
        raw.split('_').joinToString(" ") { part -> part.lowercase().replaceFirstChar { it.uppercase() } }

    private fun formatTimestamp(millis: Long, zoneId: String?): String {
        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneOffset.UTC
        return Instant.ofEpochMilli(millis).atZone(zone).format(TIMESTAMP)
    }
}
