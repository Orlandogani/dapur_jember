package com.leanecorps.dapurjember.core.printing.transport

import android.content.Context
import android.hardware.usb.UsbManager
import com.leanecorps.dapurjember.core.domain.printing.Printer
import com.leanecorps.dapurjember.core.domain.printing.PrinterLink
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DefaultPrinterTransportFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrinterTransportFactory {

    override fun create(printer: Printer): PrinterTransport = create(printer.link, printer.address)

    override fun create(link: PrinterLink, address: String): PrinterTransport = when (link) {
        PrinterLink.TCP -> TcpPrinterTransport(address)
        PrinterLink.BLUETOOTH -> BluetoothPrinterTransport(context, address)
        PrinterLink.USB -> UsbPrinterTransport(context.getSystemService(UsbManager::class.java), address)
    }
}
