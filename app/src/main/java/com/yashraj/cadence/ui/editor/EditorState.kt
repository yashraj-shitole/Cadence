package com.yashraj.cadence.ui.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.yashraj.cadence.domain.Block
import com.yashraj.cadence.domain.BlockType
import com.yashraj.cadence.domain.EditorOps
import com.yashraj.cadence.domain.Markdown
import com.yashraj.cadence.domain.Sel
import com.yashraj.cadence.domain.SlashCommand
import com.yashraj.cadence.domain.SlashToken

/** One editable block. Everything the UI reads from it is Compose state. */
@Stable
class BlockState(val id: Int, type: BlockType, text: String, checked: Boolean, indent: Int) {
    var type by mutableStateOf(type)
    var checked by mutableStateOf(checked)
    var indent by mutableIntStateOf(indent)
    var value by mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    var hasFocus by mutableStateOf(false)
    val focus = FocusRequester()
    val text: String get() = value.text

    fun toBlock() = Block(type, text, checked, indent)

    fun setText(text: String, cursor: Int = text.length) {
        value = TextFieldValue(text, TextRange(cursor.coerceIn(0, text.length)))
    }
}

/** Move focus to a block. [cursor] null means the end; [keepSelection] leaves the selection exactly as set. */
class PendingFocus(val id: Int, val cursor: Int?, val keepSelection: Boolean = false)

/**
 * The editing model behind the note editor: a list of blocks plus every way a block can change. The rules mirror
 * Notion: Enter continues lists and leaves them when empty, Backspace at the start of a block turns it into text,
 * typing a Markdown prefix converts the block, and "/" opens the command menu.
 */
@Stable
class EditorState(initial: List<Block>) {
    val blocks = mutableStateListOf<BlockState>()
    private var nextId = 0

    /** The block that last had the caret. It keeps the slash menu and toolbar pointed at the right place. */
    var focusedId by mutableIntStateOf(-1)
    var pendingFocus by mutableStateOf<PendingFocus?>(null)
    var selectedDividerId by mutableIntStateOf(-1)

    /** A slash the user dismissed: the block id and the slash index, so the menu stays closed for that one. */
    var slashDismissed by mutableStateOf<Pair<Int, Int>?>(null)

    init {
        load(initial)
    }

    val focused: BlockState? get() = blocks.firstOrNull { it.id == focusedId }

    fun toBlocks(): List<Block> = blocks.map { it.toBlock() }

    fun load(list: List<Block>) {
        blocks.clear()
        list.forEach { blocks += create(it.type, it.text, it.checked, it.indent) }
        ensureTrailingParagraph()
    }

    private fun create(type: BlockType, text: String, checked: Boolean = false, indent: Int = 0) =
        BlockState(nextId++, type, text, checked, indent)

    private fun ensureTrailingParagraph() {
        val last = blocks.lastOrNull()
        if (last == null || last.type != BlockType.Paragraph) {
            blocks += create(BlockType.Paragraph, "")
        }
    }

    private fun indexOf(block: BlockState) = blocks.indexOfFirst { it.id == block.id }

    fun focus(block: BlockState, cursor: Int? = null) {
        pendingFocus = PendingFocus(block.id, cursor)
    }

    // ---- typing ----

    fun onValueChange(block: BlockState, new: TextFieldValue) {
        val old = block.text
        if (block.type == BlockType.Code) {
            // Enter on an empty last line leaves the code block.
            if (new.text.endsWith("\n\n") && new.selection.collapsed && new.selection.start == new.text.length) {
                block.setText(new.text.trimEnd('\n'))
                val next = create(BlockType.Paragraph, "")
                blocks.add(indexOf(block) + 1, next)
                focus(next, 0)
                return
            }
            block.value = new
            return
        }
        if (new.text.contains('\n')) {
            // Enter while the slash menu is open picks its first match instead of starting a new block.
            val token = slashToken(block)
            val first = token?.let { SlashCommand.filter(it.query).firstOrNull() }
            if (token != null && first != null) {
                applySlash(block, token, first)
                return
            }
            val parts = new.text.split('\n')
            if (parts.size == 2) split(block, parts[0], parts[1]) else paste(block, new.text)
            return
        }
        if (block.type == BlockType.Paragraph && new.text.length > old.length) {
            EditorOps.shortcut(new.text)?.let { applyShortcut(block, it.type, it.checked, it.rest); return }
        }
        block.value = new
    }

    private fun applyShortcut(block: BlockState, type: BlockType, checked: Boolean, rest: String) {
        if (type == BlockType.Divider) {
            makeDivider(block)
            return
        }
        block.type = type
        block.checked = checked
        block.setText(rest)
        ensureTrailingParagraph()
        focus(block, rest.length)
    }

    /** Enter in the middle or at the end of a block. */
    private fun split(block: BlockState, before: String, after: String) {
        val i = indexOf(block)
        val leaving = block.type.continuesOnEnter && before.isEmpty() && after.isEmpty()
        if (leaving) {
            if (block.indent > 0) block.indent-- else {
                block.type = BlockType.Paragraph
                block.checked = false
            }
            block.setText("")
            focus(block, 0)
            return
        }
        val nextType = if (block.type.continuesOnEnter) block.type else BlockType.Paragraph
        block.setText(before)
        val next = create(nextType, after, indent = if (nextType.isList) block.indent else 0)
        next.value = TextFieldValue(after, TextRange(0))
        blocks.add(i + 1, next)
        focus(next, 0)
    }

    /** Pasting several lines: read them as Markdown and replace this block with the blocks they make. */
    private fun paste(block: BlockState, text: String) {
        val parsed = Markdown.parse(text)
        if (parsed.isEmpty()) {
            block.setText(text.replace('\n', ' ').trim())
            return
        }
        val i = indexOf(block)
        blocks.removeAt(i)
        val created = parsed.mapIndexed { n, b ->
            val type = if (n == 0 && b.type == BlockType.Paragraph && block.type != BlockType.Paragraph) block.type else b.type
            create(type, b.text, b.checked, b.indent)
        }
        blocks.addAll(i, created)
        ensureTrailingParagraph()
        focus(created.last())
    }

    /** Backspace with the caret at the start. Returns true when it was handled here. */
    fun backspaceAtStart(block: BlockState): Boolean {
        val i = indexOf(block)
        if (block.type != BlockType.Paragraph) {
            if (block.type.isList && block.indent > 0) block.indent-- else {
                block.type = BlockType.Paragraph
                block.checked = false
            }
            return true
        }
        if (i <= 0) return false
        val previous = blocks[i - 1]
        return when (previous.type) {
            BlockType.Divider -> {
                blocks.removeAt(i - 1)
                true
            }
            BlockType.Code -> {
                if (block.text.isEmpty()) blocks.removeAt(i)
                focus(previous)
                true
            }
            else -> {
                val join = previous.text.length
                previous.setText(previous.text + block.text, join)
                blocks.removeAt(i)
                focus(previous, join)
                true
            }
        }
    }

    // ---- blocks ----

    fun toggleType(block: BlockState, type: BlockType) {
        if (block.type == type) {
            block.type = BlockType.Paragraph
            block.checked = false
        } else {
            block.type = type
            if (!type.isList) block.indent = 0
        }
        ensureTrailingParagraph()
        focus(block, block.value.selection.end)
    }

    fun indent(block: BlockState, delta: Int) {
        if (!block.type.isList) return
        block.indent = (block.indent + delta).coerceIn(0, Markdown.MaxIndent)
    }

    private fun makeDivider(block: BlockState) {
        val i = indexOf(block)
        block.type = BlockType.Divider
        block.checked = false
        block.indent = 0
        block.setText("")
        val following = blocks.getOrNull(i + 1)
        if (following != null && following.type == BlockType.Paragraph) {
            focus(following, 0)
        } else {
            val paragraph = create(BlockType.Paragraph, "")
            blocks.add(i + 1, paragraph)
            focus(paragraph, 0)
        }
    }

    fun removeDivider(block: BlockState) {
        blocks.removeAll { it.id == block.id }
        selectedDividerId = -1
        ensureTrailingParagraph()
    }

    /** A tap below the last block: carry on writing there. */
    fun tapBelow() {
        val last = blocks.last()
        if (last.type == BlockType.Paragraph && last.text.isEmpty()) {
            focus(last)
        } else {
            val next = create(BlockType.Paragraph, "")
            blocks += next
            focus(next, 0)
        }
    }

    // ---- inline formatting ----

    private fun replaceSelection(block: BlockState, result: Sel) {
        block.value = TextFieldValue(result.text, TextRange(result.start, result.end))
        pendingFocus = PendingFocus(block.id, null, keepSelection = true)
    }

    private fun selectionOf(block: BlockState): Sel {
        val s = block.value.selection
        return Sel(block.text, s.min, s.max)
    }

    /** Wraps (or unwraps) the selection in an inline marker such as `**`. */
    fun toggleInline(block: BlockState, open: String, close: String = open) {
        replaceSelection(block, EditorOps.toggleInline(selectionOf(block), open, close))
    }

    fun insertLink(block: BlockState) {
        replaceSelection(block, EditorOps.link(selectionOf(block)))
    }

    fun insertSlash(block: BlockState) {
        val s = block.value.selection
        val needsSpace = s.start > 0 && !block.text[s.start - 1].isWhitespace()
        val insert = if (needsSpace) " /" else "/"
        val text = block.text.substring(0, s.start) + insert + block.text.substring(s.end)
        block.setText(text, s.start + insert.length)
        focus(block, s.start + insert.length)
    }

    // ---- slash menu ----

    /** The "/command" being typed in [block], unless the user dismissed that very slash. */
    fun slashToken(block: BlockState? = focused): SlashToken? {
        if (block == null || block.type == BlockType.Code || !block.value.selection.collapsed) return null
        val token = EditorOps.slash(block.text, block.value.selection.start) ?: return null
        return if (slashDismissed == (block.id to token.from)) null else token
    }

    fun applySlash(block: BlockState, token: SlashToken, command: SlashCommand) {
        val cleaned = block.text.removeRange(token.from, token.to)
        val caret = token.from
        val type = command.blockType
        when {
            type == BlockType.Divider -> {
                if (cleaned.isBlank()) {
                    makeDivider(block)
                } else {
                    block.setText(cleaned, caret)
                    val i = indexOf(block)
                    val divider = create(BlockType.Divider, "")
                    val paragraph = create(BlockType.Paragraph, "")
                    blocks.addAll(i + 1, listOf(divider, paragraph))
                    focus(paragraph, 0)
                }
            }
            type != null -> {
                block.type = type
                block.checked = false
                if (!type.isList) block.indent = 0
                block.setText(cleaned, caret)
                ensureTrailingParagraph()
                focus(block, caret)
            }
            command == SlashCommand.Link -> {
                block.setText(cleaned, caret)
                replaceSelection(block, EditorOps.link(Sel(cleaned, caret, caret)))
            }
            else -> {
                block.setText(cleaned, caret)
                val (open, close) = when (command) {
                    SlashCommand.Bold -> "**" to "**"
                    SlashCommand.Italic -> "*" to "*"
                    SlashCommand.Strike -> "~~" to "~~"
                    else -> "`" to "`"
                }
                replaceSelection(block, EditorOps.toggleInline(Sel(cleaned, caret, caret), open, close))
            }
        }
    }
}
