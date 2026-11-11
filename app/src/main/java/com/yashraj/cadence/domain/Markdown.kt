package com.yashraj.cadence.domain

/** The kinds of block a note is made of. Each maps to one Markdown construct. */
enum class BlockType {
    Paragraph, Heading1, Heading2, Heading3, Bullet, Numbered, Todo, Quote, Code, Divider;

    val isList: Boolean get() = this == Bullet || this == Numbered || this == Todo

    /** Pressing Enter at the end continues the same kind of block. */
    val continuesOnEnter: Boolean get() = isList || this == Quote

    val isHeading: Boolean get() = this == Heading1 || this == Heading2 || this == Heading3
}

/** One block. [text] holds inline Markdown (`**bold**`, `*italic*`, `~~strike~~`, `code`, `[link](url)`). */
data class Block(
    val type: BlockType = BlockType.Paragraph,
    val text: String = "",
    val checked: Boolean = false,
    val indent: Int = 0,
)

enum class InlineKind { Bold, Italic, Strike, Code, Link }

/** One inline construct. All ranges are [start, end) over the raw text, so markers can be hidden or dimmed. */
data class InlineSpan(
    val kind: InlineKind,
    val openStart: Int,
    val openEnd: Int,
    val contentStart: Int,
    val contentEnd: Int,
    val closeStart: Int,
    val closeEnd: Int,
    val url: String? = null,
)

object Inline {
    private val code = Regex("""`([^`\n]+)`""")
    private val link = Regex("""\[([^\]\n]+)]\(([^)\n]*)\)""")
    private val boldStar = Regex("""\*\*(?=\S)(.+?)(?<=\S)\*\*""")
    private val boldUnder = Regex("""(?<!\w)__(?=\S)(.+?)(?<=\S)__(?!\w)""")
    private val strike = Regex("""~~(?=\S)(.+?)(?<=\S)~~""")
    private val italicStar = Regex("""(?<![\w\\*])\*(?=[^\s*])([^*\n]+?)(?<=\S)\*(?![\w*])""")
    private val italicUnder = Regex("""(?<![\w_])_(?=[^\s_])([^_\n]+?)(?<=\S)_(?![\w_])""")

    private fun overlaps(a1: Int, a2: Int, b1: Int, b2: Int) = a1 < b2 && b1 < a2

    /** Finds every inline construct. Code and link targets are left alone by the other passes. */
    fun spans(text: String): List<InlineSpan> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<InlineSpan>()
        val blocked = mutableListOf<Pair<Int, Int>>()

        fun pass(regex: Regex, kind: InlineKind, openLen: Int, closeLen: Int, block: Boolean = false) {
            for (m in regex.findAll(text)) {
                val s = m.range.first
                val e = m.range.last + 1
                if (blocked.any { overlaps(s, e, it.first, it.second) }) continue
                out += InlineSpan(kind, s, s + openLen, s + openLen, e - closeLen, e - closeLen, e)
                if (block) blocked += s to e
            }
        }

        pass(code, InlineKind.Code, 1, 1, block = true)
        for (m in link.findAll(text)) {
            val s = m.range.first
            val e = m.range.last + 1
            if (blocked.any { overlaps(s, e, it.first, it.second) }) continue
            val content = m.groups[1]!!.range
            out += InlineSpan(InlineKind.Link, s, s + 1, content.first, content.last + 1, content.last + 1, e, m.groups[2]!!.value)
            blocked += (content.last + 1) to e
        }
        pass(boldStar, InlineKind.Bold, 2, 2)
        pass(boldUnder, InlineKind.Bold, 2, 2)
        pass(strike, InlineKind.Strike, 2, 2)
        pass(italicStar, InlineKind.Italic, 1, 1)
        pass(italicUnder, InlineKind.Italic, 1, 1)
        return out.sortedBy { it.openStart }
    }

    /** The text with every marker removed. */
    fun strip(text: String): String {
        val spans = spans(text)
        if (spans.isEmpty()) return text
        val hidden = hidden(text.length, spans)
        return buildString { text.forEachIndexed { i, c -> if (!hidden[i]) append(c) } }
    }

    /** Which characters of the raw text are markers. */
    fun hidden(length: Int, spans: List<InlineSpan>): BooleanArray {
        val hidden = BooleanArray(length)
        spans.forEach { sp ->
            for (i in sp.openStart until sp.openEnd) hidden[i] = true
            for (i in sp.closeStart until sp.closeEnd) hidden[i] = true
        }
        return hidden
    }
}

/**
 * A note is stored as plain Markdown so it stays readable anywhere: headings, bulleted, numbered and
 * `- [ ]` task lists with two-space nesting, `>` quotes, fenced code, `---` dividers, and inline emphasis.
 */
object Markdown {
    const val MaxIndent = 3

    private val divider = Regex("""^(-{3,}|\*{3,}|_{3,})\s*$""")
    private val todo = Regex("""^(\s*)[-*+] \[([ xX])](?: (.*))?$""")
    private val bullet = Regex("""^(\s*)[-*+](?: (.*))?$""")
    private val numbered = Regex("""^(\s*)\d+[.)](?: (.*))?$""")
    private val heading = Regex("""^(#{1,6}) (.*)$""")
    private val quote = Regex("""^> ?(.*)$""")

    private fun indentOf(lead: String): Int = (lead.fold(0) { acc, c -> acc + if (c == '\t') 2 else 1 } / 2).coerceIn(0, MaxIndent)

    fun parse(source: String): List<Block> {
        if (source.isBlank()) return emptyList()
        val lines = source.replace("\r\n", "\n").split('\n')
        val blocks = mutableListOf<Block>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> Unit
                trimmed.startsWith("```") -> {
                    val body = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) body += lines[i++]
                    blocks += Block(BlockType.Code, body.joinToString("\n"))
                }
                divider.matches(line) -> blocks += Block(BlockType.Divider)
                else -> {
                    val t = todo.matchEntire(line)
                    val b = bullet.matchEntire(line)
                    val n = numbered.matchEntire(line)
                    val h = heading.matchEntire(line)
                    val q = quote.matchEntire(line)
                    blocks += when {
                        t != null -> Block(BlockType.Todo, t.groupValues[3], t.groupValues[2] != " ", indentOf(t.groupValues[1]))
                        b != null -> Block(BlockType.Bullet, b.groupValues[2], indent = indentOf(b.groupValues[1]))
                        n != null -> Block(BlockType.Numbered, n.groupValues[2], indent = indentOf(n.groupValues[1]))
                        h != null -> Block(
                            when (h.groupValues[1].length) { 1 -> BlockType.Heading1; 2 -> BlockType.Heading2; else -> BlockType.Heading3 },
                            h.groupValues[2],
                        )
                        q != null -> Block(BlockType.Quote, q.groupValues[1])
                        else -> Block(BlockType.Paragraph, line.trimEnd())
                    }
                }
            }
            i++
        }
        return blocks
    }

    /** The number shown beside each numbered block (0 for every other block). */
    fun numbering(blocks: List<Block>): IntArray {
        val counters = IntArray(MaxIndent + 1)
        return IntArray(blocks.size) { index ->
            val b = blocks[index]
            if (!b.type.isList) {
                counters.fill(0)
                0
            } else {
                for (j in b.indent + 1..MaxIndent) counters[j] = 0
                if (b.type == BlockType.Numbered) ++counters[b.indent] else { counters[b.indent] = 0; 0 }
            }
        }
    }

    private fun sameGroup(a: Block, b: Block) = (a.type.isList && b.type.isList) || (a.type == BlockType.Quote && b.type == BlockType.Quote)

    fun serialize(blocks: List<Block>): String {
        val kept = blocks.filter { !(it.type == BlockType.Paragraph && it.text.isEmpty()) }
        val numbers = numbering(kept)
        val out = StringBuilder()
        kept.forEachIndexed { index, b ->
            val pad = "  ".repeat(b.indent.coerceIn(0, MaxIndent))
            val line = when (b.type) {
                BlockType.Paragraph -> b.text
                BlockType.Heading1 -> "# ${b.text}"
                BlockType.Heading2 -> "## ${b.text}"
                BlockType.Heading3 -> "### ${b.text}"
                BlockType.Bullet -> "$pad- ${b.text}"
                BlockType.Numbered -> "$pad${numbers[index]}. ${b.text}"
                BlockType.Todo -> "$pad- [${if (b.checked) "x" else " "}] ${b.text}"
                BlockType.Quote -> "> ${b.text}"
                BlockType.Code -> "```\n${b.text}\n```"
                BlockType.Divider -> "---"
            }
            if (index > 0) out.append(if (sameGroup(kept[index - 1], b)) "\n" else "\n\n")
            out.append(line)
        }
        return out.toString()
    }

    /** Done and total for the to-do items, or null when there are none. */
    fun progress(blocks: List<Block>): Pair<Int, Int>? {
        val items = blocks.filter { it.type == BlockType.Todo }
        if (items.isEmpty()) return null
        return items.count { it.checked } to items.size
    }

    /** "2 of 5 done · Charger and the long cable..." for a note card: finished items are counted, not repeated. */
    fun preview(source: String): String {
        val blocks = parse(source)
        val progress = progress(blocks)
        val text = blocks
            .filter { !(it.type == BlockType.Todo && it.checked) && it.type != BlockType.Divider && it.type != BlockType.Code }
            .map { Inline.strip(it.text).trim() }
            .filter { it.isNotEmpty() }
            .joinToString(". ") { it.trimEnd('.') }
            .let { if (it.isNotEmpty()) "$it." else it }
        return when {
            progress != null && text.isNotEmpty() -> "${progress.first} of ${progress.second} done · $text"
            progress != null -> "${progress.first} of ${progress.second} done"
            else -> text
        }
    }
}
