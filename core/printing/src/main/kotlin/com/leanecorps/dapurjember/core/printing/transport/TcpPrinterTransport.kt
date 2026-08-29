package com.leanecorps.dapurjember.core.printing.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

private const val DEFAULT_PORT = 9100
private const val CONNECT_TIMEOUT_MS = 5_000
private const val WRITE_TIMEOUT_MS = 10_000

/**
 * Raw socket to an ESC/POS network printer (architecture §6 — "TCP:9100"). [address] is
 * `host` or `host:port`; the port defaults to 9100.
 */
class TcpPrinterTransport(private val address: String) : PrinterTransport {

    override suspend fun send(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val (host, port) = parse(address)
            runCatching {
                Socket().use { socket ->
                    socket.soTimeout = WRITE_TIMEOUT_MS
                    socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                    socket.getOutputStream().apply {
                        write(bytes)
                        flush()
                    }
                }
            }.getOrElse { throw PrinterTransportException("TCP print to $address failed: ${it.message}", it) }
        }
    }

    private fun parse(value: String): Pair<String, Int> {
        val idx = value.lastIndexOf(':')
        return if (idx > 0) {
            value.substring(0, idx) to (value.substring(idx + 1).toIntOrNull() ?: DEFAULT_PORT)
        } else {
            value to DEFAULT_PORT
        }
    }
}
