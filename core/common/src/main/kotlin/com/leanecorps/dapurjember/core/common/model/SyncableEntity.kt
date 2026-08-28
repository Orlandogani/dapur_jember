package com.leanecorps.dapurjember.core.common.model

/**
 * The envelope every persisted entity carries (docs/3-data-model section 1).
 *
 * It enables soft delete now and, later, offline multi-device sync. Nothing in v1 reads
 * [deviceId] or [revision] — that is intentional; every table still stores them so v2's
 * sync engine is an additive feature rather than a schema rewrite.
 */
interface SyncableEntity {
    /** UUIDv7, generated client-side. */
    val id: String

    /** Epoch millis, UTC. */
    val createdAt: Long

    /** Epoch millis, UTC. */
    val updatedAt: Long

    /** Soft delete — `null` means the row is live. */
    val deletedAt: Long?

    /** Id of the device that originated the record. */
    val deviceId: String

    /** Optimistic-concurrency counter. */
    val revision: Int
}
