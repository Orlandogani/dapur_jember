package com.leanecorps.dapurjember.core.printing.transport

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TRANSFER_TIMEOUT_MS = 10_000

/**
 * USB-OTG transport. [address] is the [UsbDevice.getDeviceName]. The USB permission dance
 * (`UsbManager.requestPermission`) happens in the printer-setup flow; by the time a print is
 * attempted the device must already be granted, or this throws.
 */
class UsbPrinterTransport(
    private val usbManager: UsbManager,
    private val address: String,
) : PrinterTransport {

    override suspend fun send(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val device = usbManager.deviceList.values.firstOrNull { it.deviceName == address }
            ?: throw PrinterTransportException("USB printer $address is not connected")
        if (!usbManager.hasPermission(device)) {
            throw PrinterTransportException("USB permission not granted for $address")
        }

        val (iface, endpoint) = bulkOutEndpoint(device)
            ?: throw PrinterTransportException("USB printer $address has no bulk-out endpoint")

        val connection = usbManager.openDevice(device)
            ?: throw PrinterTransportException("Could not open USB printer $address")

        runCatching {
            connection.claimInterface(iface, true)
            val sent = connection.bulkTransfer(endpoint, bytes, bytes.size, TRANSFER_TIMEOUT_MS)
            if (sent < 0) throw PrinterTransportException("USB bulk transfer to $address failed")
        }.also {
            connection.releaseInterface(iface)
            connection.close()
        }.getOrThrow()
    }

    private fun bulkOutEndpoint(device: UsbDevice) = (0 until device.interfaceCount)
        .map { device.getInterface(it) }
        .firstNotNullOfOrNull { iface ->
            (0 until iface.endpointCount)
                .map { iface.getEndpoint(it) }
                .firstOrNull {
                    it.direction == UsbConstants.USB_DIR_OUT && it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                }
                ?.let { iface to it }
        }
}
