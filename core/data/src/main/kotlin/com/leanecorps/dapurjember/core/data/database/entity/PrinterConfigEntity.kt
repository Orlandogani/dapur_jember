package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A configured printer (`docs/3-data-model` §3.8). Device-local infrastructure — a printer
 * paired to this tablet is not reachable from a peer, so this is not synced. Soft-deleted so
 * a removed printer stops being a print target without vanishing from job history.
 */
@Entity(tableName = "printer_config")
data class PrinterConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** BLUETOOTH / USB / TCP. */
    val transport: String,
    /** Bluetooth MAC, USB device name, or `host:port` for TCP. */
    val address: String,
    @ColumnInfo("paper_width_mm") val paperWidthMm: Int,
    /** ESC/POS character code table (ESC t n). */
    val codepage: Int = 0,
    /** Comma-separated roles this printer serves: KITCHEN, BAR, RECEIPT. */
    val roles: String,
    @ColumnInfo("created_at") val createdAt: Long,
    @ColumnInfo("updated_at") val updatedAt: Long,
    @ColumnInfo("deleted_at") val deletedAt: Long? = null,
)
