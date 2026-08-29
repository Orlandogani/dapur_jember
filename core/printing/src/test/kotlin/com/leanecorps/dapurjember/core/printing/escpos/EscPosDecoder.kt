package com.leanecorps.dapurjember.core.printing.escpos

/**
 * Test-only: strips ESC/POS control sequences from a job and returns the visible text lines,
 * so template tests can assert on layout without hard-coding byte offsets.
 */
object EscPosDecoder {

    fun lines(job: ByteArray): List<String> = text(job).split('\n')

    fun text(job: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < job.size) {
            val byte = job[i]
            when (byte) {
                EscPos.ESC, EscPos.GS -> i += skipLength(job, i)
                EscPos.LF -> {
                    sb.append('\n')
                    i++
                }

                else -> {
                    sb.append((byte.toInt() and 0xFF).toChar())
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun skipLength(job: ByteArray, at: Int): Int {
        val marker = job[at]
        val cmd = job.getOrNull(at + 1)?.toInt()?.toChar() ?: return 1
        return when {
            marker == EscPos.ESC && cmd == '@' -> 2
            marker == EscPos.ESC && cmd in listOf('a', 'E', '-', 't', 'd') -> 3
            marker == EscPos.ESC && cmd == 'p' -> 4
            marker == EscPos.GS && cmd == '!' -> 3
            marker == EscPos.GS && cmd == 'V' -> 4
            else -> 2
        }
    }
}
