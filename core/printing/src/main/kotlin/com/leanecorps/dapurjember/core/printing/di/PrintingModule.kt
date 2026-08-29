package com.leanecorps.dapurjember.core.printing.di

import com.leanecorps.dapurjember.core.domain.printing.PrintQueueScheduler
import com.leanecorps.dapurjember.core.domain.printing.TicketRenderer
import com.leanecorps.dapurjember.core.printing.DefaultTicketRenderer
import com.leanecorps.dapurjember.core.printing.transport.DefaultPrinterTransportFactory
import com.leanecorps.dapurjember.core.printing.transport.PrinterTransportFactory
import com.leanecorps.dapurjember.core.printing.work.WorkManagerPrintQueueScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PrintingModule {

    @Binds
    abstract fun bindTicketRenderer(impl: DefaultTicketRenderer): TicketRenderer

    @Binds
    abstract fun bindPrinterTransportFactory(impl: DefaultPrinterTransportFactory): PrinterTransportFactory

    @Binds
    abstract fun bindPrintQueueScheduler(impl: WorkManagerPrintQueueScheduler): PrintQueueScheduler
}
