package com.leanecorps.dapurjember.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leanecorps.dapurjember.core.common.model.SyncableEntity

/** Single-row table (`docs/3-data-model` 3.1) holding the restaurant's configuration. */
@Entity(tableName = "store_profile")
data class StoreProfileEntity(
    @PrimaryKey override val id: String,
    val name: String,
    val address: String?,
    val phone: String?,
    @ColumnInfo("tax_id") val taxId: String?,
    @ColumnInfo("currency_code") val currencyCode: String,
    @ColumnInfo("currency_minor_units") val currencyMinorUnits: Int,
    @ColumnInfo("tax_rate_bp") val taxRateBp: Int,
    @ColumnInfo("tax_inclusive") val taxInclusive: Boolean,
    @ColumnInfo("service_charge_bp") val serviceChargeBp: Int,
    @ColumnInfo("service_charge_taxable") val serviceChargeTaxable: Boolean,
    @ColumnInfo("rounding_mode") val roundingMode: String,
    @ColumnInfo("business_day_cutoff_min") val businessDayCutoffMin: Int,
    @ColumnInfo("timezone_id") val timezoneId: String,
    @ColumnInfo("receipt_header") val receiptHeader: String?,
    @ColumnInfo("receipt_footer") val receiptFooter: String?,
    @ColumnInfo("created_at") override val createdAt: Long,
    @ColumnInfo("updated_at") override val updatedAt: Long,
    @ColumnInfo("deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo("device_id") override val deviceId: String,
    override val revision: Int = 1,
) : SyncableEntity
