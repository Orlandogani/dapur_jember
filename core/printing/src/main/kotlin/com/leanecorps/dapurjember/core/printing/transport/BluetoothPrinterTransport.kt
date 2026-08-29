package com.leanecorps.dapurjember.core.printing.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** Serial Port Profile — the service every ESC/POS Bluetooth printer advertises. */
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Classic-Bluetooth SPP transport — "90% of real-world printers" (architecture §6). [address]
 * is the printer's MAC. Requires the `BLUETOOTH_CONNECT` runtime permission (API 31+); the
 * caller is responsible for having obtained it before a print is attempted.
 */
class BluetoothPrinterTransport(
    private val context: Context,
    private val address: String,
) : PrinterTransport {

    @SuppressLint("MissingPermission") // permission is gated in the settings/printer-setup flow
    override suspend fun send(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                ?: throw PrinterTransportException("This device has no Bluetooth adapter")
            if (!adapter.isEnabled) throw PrinterTransportException("Bluetooth is turned off")

            val device = runCatching { adapter.getRemoteDevice(address) }
                .getOrElse { throw PrinterTransportException("Unknown Bluetooth printer $address", it) }

            runCatching {
                device.createRfcommSocketToServiceRecord(SPP_UUID).use { socket ->
                    cancelDiscovery(adapter)
                    socket.connect()
                    socket.outputStream.apply {
                        write(bytes)
                        flush()
                    }
                }
            }.getOrElse {
                throw PrinterTransportException("Bluetooth print to $address failed: ${it.message}", it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelDiscovery(adapter: BluetoothAdapter) {
        runCatching { adapter.cancelDiscovery() }
    }
}
