package com.leanecorps.dapurjember.core.printing.escpos

/** Greedy word-wrap to [width]. Existing newlines are preserved; a word wider than [width] is hard-split. */
internal fun wrapText(value: String, width: Int): List<String> =
    value.split('\n').flatMap { wrapParagraph(it, width) }

private fun wrapParagraph(paragraph: String, width: Int): List<String> {
    if (paragraph.isEmpty()) return listOf("")

    val lines = mutableListOf<String>()
    var current = StringBuilder()

    for (word in paragraph.split(' ')) {
        when {
            word.length > width -> {
                if (current.isNotEmpty()) {
                    lines += current.toString()
                    current = StringBuilder()
                }
                val chunks = word.chunked(width)
                chunks.dropLast(1).forEach { lines += it }
                current.append(chunks.last())
                if (current.length == width) {
                    lines += current.toString()
                    current = StringBuilder()
                }
            }

            current.isEmpty() -> current.append(word)
            current.length + 1 + word.length <= width -> current.append(' ').append(word)
            else -> {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
    }
    lines += current.toString()
    return lines
}
