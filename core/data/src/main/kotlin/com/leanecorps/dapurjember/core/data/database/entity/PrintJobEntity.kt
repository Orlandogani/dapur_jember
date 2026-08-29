package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A queued print job (`docs/3-data-model` §3.8). The [PrintQueue] sits between a completed
 * sale and the printer: a dead printer, empty paper tray or flat battery must never roll back
 * the sale (FR-PR3), so the sale commits and the job retries here.
 *
 * Device-local infrastructure like `change_log` — **not** a `SyncableEntity`, no `change_log`
 * row per state change. Jobs are hard-deleted once printed and pruned.
 */
@Entity(
    tableName = "print_job",
    indices = [Index("state"), Index("created_at")],
)
data class PrintJobEntity(
    @PrimaryKey val id: String,
    /** KITCHEN / BAR / RECEIPT / ZREPORT. */
    val type: String,
    /** Fully-rendered ESC/POS bytes, ready to write to the transport. */
    @ColumnInfo("payload_bytes") val payloadBytes: ByteArray,
    /** Which configured printer to send to; null = the default printer for [type]'s role. */
    @ColumnInfo("target_printer_id") val targetPrinterId: String? = null,
    /** PENDING / PRINTING / DONE / FAILED. */
    val state: String,
    val attempts: Int = 0,
    @ColumnInfo("last_error") val lastError: String? = null,
    @ColumnInfo("created_at") val createdAt: Long,
    @ColumnInfo("updated_at") val updatedAt: Long,
) {
    // Room entities with a BLOB column: identity comparison is by id only.
    override fun equals(other: Any?): Boolean = this === other || (other is PrintJobEntity && other.id == id)

    override fun hashCode(): Int = id.hashCode()
}
