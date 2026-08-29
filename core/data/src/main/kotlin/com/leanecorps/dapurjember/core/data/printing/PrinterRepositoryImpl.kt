package com.leanecorps.dapurjember.core.data.printing

import com.leanecorps.dapurjember.core.common.time.TimeProvider
import com.leanecorps.dapurjember.core.data.database.dao.PrinterConfigDao
import com.leanecorps.dapurjember.core.data.database.entity.PrinterConfigEntity
import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import com.leanecorps.dapurjember.core.domain.printing.PrinterRepository
import com.leanecorps.dapurjember.core.domain.printing.PrinterRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal fun PrinterConfigEntity.toDomain() = Printer(
    id = id,
    name = name,
    link = runCatching { PrinterLink.valueOf(transport) }.getOrDefault(PrinterLink.TCP),
    address = address,
    paperWidthMm = paperWidthMm,
    codepage = codepage,
    roles = roles.split(',')
        .mapNotNull { token -> runCatching { PrinterRole.valueOf(token.trim()) }.getOrNull() }
        .toSet(),
)

internal fun Printer.toEntity(existing: PrinterConfigEntity?, now: Long) = PrinterConfigEntity(
    id = id,
    name = name,
    transport = link.name,
    address = address,
    paperWidthMm = paperWidthMm,
    codepage = codepage,
    roles = roles.joinToString(",") { it.name },
    createdAt = existing?.createdAt ?: now,
    updatedAt = now,
    deletedAt = existing?.deletedAt,
)

internal class PrinterRepositoryImpl @Inject constructor(
    private val dao: PrinterConfigDao,
    private val time: TimeProvider,
) : PrinterRepository {

    override fun observePrinters(): Flow<List<Printer>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPrinter(id: String): Printer? = dao.getById(id)?.toDomain()

    override suspend fun printersForRole(role: PrinterRole): List<Printer> =
        dao.forRole(role.name).map { it.toDomain() }

    override suspend fun savePrinter(printer: Printer) {
        val existing = dao.getById(printer.id)
        dao.upsert(printer.toEntity(existing, time.nowMillis()))
    }

    override suspend fun removePrinter(id: String) = dao.softDelete(id, time.nowMillis())
}
