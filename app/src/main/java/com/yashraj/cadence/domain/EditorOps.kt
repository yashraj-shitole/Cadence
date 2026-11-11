package com.yashraj.cadence.domain

/** Text with a selection: [start] <= [end]; equal means a caret. */
data class Sel(val text: String, val start: Int, val end: Int)

/** What typing a Markdown prefix at the start of a paragraph turns it into. */
data class Shortcut(val type: BlockType, val checked: Boolean, val rest: String)

/** A "/command" being typed: [from] is the index of the slash, [to] the caret. */
data class SlashToken(val from: Int, val to: Int, val query: String)

/** The commands in the slash menu. Block commands change the block; inline commands wrap or insert formatting. */
enum class SlashCommand(
    val title: String,
    val hint: String,
    val keywords: String,
    val blockType: BlockType? = null,
) {
    Text("Text", "Plain paragraph", "paragraph text plain p", BlockType.Paragraph),
    Heading1("Heading 1", "Big section heading", "h1 heading title large #", BlockType.Heading1),
    Heading2("Heading 2", "Medium section heading", "h2 heading subtitle medium ##", BlockType.Heading2),
    Heading3("Heading 3", "Small section heading", "h3 heading small ###", BlockType.Heading3),
    Bullet("Bulleted list", "A simple list", "bullet bulleted list unordered ul -", BlockType.Bullet),
    Numbered("Numbered list", "A list with numbers", "numbered ordered list ol 1.", BlockType.Numbered),
    Todo("To-do list", "Track tasks with checkboxes", "todo to-do task check checkbox checklist []", BlockType.Todo),
    Quote("Quote", "Set a passage apart", "quote blockquote cite >", BlockType.Quote),
    Code("Code block", "Monospaced code", "code snippet pre ```", BlockType.Code),
    Divider("Divider", "A line between sections", "divider rule line hr separator ---", BlockType.Divider),
    Bold("Bold", "Strong emphasis", "bold strong **"),
    Italic("Italic", "Light emphasis", "italic emphasis em *"),
    Strike("Strikethrough", "Cross text out", "strike strikethrough delete ~~"),
    InlineCode("Inline code", "Code inside a sentence", "code inline mono `"),
    Link("Link", "Link to a web address", "link url href web");

    companion object {
        /** Commands matching what has been typed after the slash, best matches first. */
        fun filter(query: String): List<SlashCommand> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return entries
            return entries
                .filter { c -> c.title.lowercase().contains(q) || c.keywords.lowercase().split(' ').any { it.startsWith(q) } }
                .sortedBy { c -> if (c.title.lowercase().startsWith(q)) 0 else 1 }
        }
    }
}

object EditorOps {
    private val numberedPrefix = Regex("""^\d{1,3}[.)] """)

    /**
     * Typing a Markdown prefix at the start of a paragraph converts it: "# ", "## ", "### ", "- ", "* ", "1. ",
     * "[] ", "[x] ", "> ", "```", and a lone "---".
     */
    fun shortcut(text: String): Shortcut? = when {
        text == "---" -> Shortcut(BlockType.Divider, false, "")
        text.startsWith("### ") -> Shortcut(BlockType.Heading3, false, text.removePrefix("### "))
        text.startsWith("## ") -> Shortcut(BlockType.Heading2, false, text.removePrefix("## "))
        text.startsWith("# ") -> Shortcut(BlockType.Heading1, false, text.removePrefix("# "))
        text.startsWith("- [ ] ") -> Shortcut(BlockType.Todo, false, text.removePrefix("- [ ] "))
        text.startsWith("- [x] ") -> Shortcut(BlockType.Todo, true, text.removePrefix("- [x] "))
        text.startsWith("[] ") -> Shortcut(BlockType.Todo, false, text.removePrefix("[] "))
        text.startsWith("[ ] ") -> Shortcut(BlockType.Todo, false, text.removePrefix("[ ] "))
        text.startsWith("[x] ") -> Shortcut(BlockType.Todo, true, text.removePrefix("[x] "))
        text.startsWith("- ") -> Shortcut(BlockType.Bullet, false, text.removePrefix("- "))
        text.startsWith("* ") -> Shortcut(BlockType.Bullet, false, text.removePrefix("* "))
        text.startsWith("+ ") -> Shortcut(BlockType.Bullet, false, text.removePrefix("+ "))
        numberedPrefix.containsMatchIn(text) -> Shortcut(BlockType.Numbered, false, text.replaceFirst(numberedPrefix, ""))
        text.startsWith("> ") -> Shortcut(BlockType.Quote, false, text.removePrefix("> "))
        text.startsWith("```") -> Shortcut(BlockType.Code, false, text.removePrefix("```").trimStart())
        else -> null
    }

    /**
     * Finds a "/command" ending at [cursor]: a slash at the start of the block or after a space, followed by
     * query text with no leading space. "http://" and "and/or" are not commands.
     */
    fun slash(text: String, cursor: Int): SlashToken? {
        if (cursor < 1 || cursor > text.length) return null
        var i = cursor - 1
        while (i >= 0 && text[i] != '/' && text[i] != '\n') i--
        if (i < 0 || text[i] != '/') return null
        if (i > 0 && !text[i - 1].isWhitespace()) return null
        val query = text.substring(i + 1, cursor)
        if (query.isNotEmpty() && query[0].isWhitespace()) return null
        if (query.length > 24) return null
        return SlashToken(i, cursor, query)
    }

    /**
     * Wraps the selection in [open] and [close], or unwraps it when it is already wrapped. With a caret it
     * inserts an empty pair and puts the caret between.
     */
    fun toggleInline(sel: Sel, open: String, close: String = open): Sel {
        val (text, s, e) = sel
        if (s == e) {
            return Sel(text.substring(0, s) + open + close + text.substring(s), s + open.length, s + open.length)
        }
        val before = text.substring(0, s)
        val mid = text.substring(s, e)
        val after = text.substring(e)
        // A single * or _ must not be mistaken for half of a ** or __.
        val single = open.length == 1
        val wrappedOutside = before.endsWith(open) && after.startsWith(close) &&
            !(single && (before.endsWith(open + open) || after.startsWith(close + close)))
        return when {
            wrappedOutside ->
                Sel(before.dropLast(open.length) + mid + after.drop(close.length), s - open.length, e - open.length)
            mid.length >= open.length + close.length && mid.startsWith(open) && mid.endsWith(close) -> {
                val inner = mid.substring(open.length, mid.length - close.length)
                Sel(before + inner + after, s, s + inner.length)
            }
            else -> Sel(before + open + mid + close + after, s + open.length, e + open.length)
        }
    }

    /** Turns the selection into a link and puts the caret where the address goes. */
    fun link(sel: Sel): Sel {
        val (text, s, e) = sel
        val mid = text.substring(s, e)
        val result = text.substring(0, s) + "[" + mid + "]()" + text.substring(e)
        val caret = if (mid.isEmpty()) s + 1 else s + 1 + mid.length + 2
        return Sel(result, caret, caret)
    }
}
