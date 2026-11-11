package com.yashraj.cadence.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.yashraj.cadence.domain.Block
import com.yashraj.cadence.domain.BlockType
import com.yashraj.cadence.domain.SlashCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorStateTest {
    private fun state(vararg blocks: Block) = EditorState(blocks.toList())

    private fun BlockState.type(text: String, cursor: Int = text.length) =
        TextFieldValue(text, TextRange(cursor))

    private fun EditorState.types() = blocks.map { it.type }
    private fun EditorState.texts() = blocks.map { it.text }

    @Test
    fun alwaysEndsWithAnEmptyParagraphToKeepWritingIn() {
        val s = state(Block(BlockType.Heading1, "Title"))
        assertEquals(listOf(BlockType.Heading1, BlockType.Paragraph), s.types())
        assertEquals(listOf(BlockType.Paragraph), EditorState(emptyList()).types())
    }

    @Test
    fun enterContinuesAListAndLeavesItWhenEmpty() {
        val s = state(Block(BlockType.Bullet, "milk"))
        val b = s.blocks[0]
        s.onValueChange(b, b.type("milk\n"))
        assertEquals(listOf(BlockType.Bullet, BlockType.Bullet, BlockType.Paragraph), s.types())
        assertEquals("milk", b.text)
        // Enter on the new, empty item turns it into text.
        val empty = s.blocks[1]
        s.onValueChange(empty, empty.type("\n", 0))
        assertEquals(BlockType.Paragraph, empty.type)
    }

    @Test
    fun enterAfterAHeadingStartsAParagraphAndSplitsTextAtTheCaret() {
        val s = state(Block(BlockType.Heading2, "Heading"))
        val h = s.blocks[0]
        s.onValueChange(h, h.type("Head\ning", 5))
        assertEquals(listOf("Head", "ing", ""), s.texts())
        assertEquals(BlockType.Paragraph, s.blocks[1].type)
        assertEquals(BlockType.Heading2, h.type)
    }

    @Test
    fun enterOnAnEmptyIndentedItemOutdentsFirst() {
        val s = state(Block(BlockType.Bullet, "", indent = 1))
        val b = s.blocks[0]
        s.onValueChange(b, b.type("\n", 0))
        assertEquals(BlockType.Bullet, b.type)
        assertEquals(0, b.indent)
    }

    @Test
    fun newTodoItemsStartUnchecked() {
        val s = state(Block(BlockType.Todo, "ship it", checked = true))
        val b = s.blocks[0]
        s.onValueChange(b, b.type("ship it\n"))
        assertTrue(b.checked)
        assertFalse(s.blocks[1].checked)
        assertEquals(BlockType.Todo, s.blocks[1].type)
    }

    @Test
    fun typingAMarkdownPrefixConvertsTheParagraph() {
        val s = state(Block(BlockType.Paragraph, ""))
        val b = s.blocks[0]
        s.onValueChange(b, b.type("# "))
        assertEquals(BlockType.Heading1, b.type)
        assertEquals("", b.text)
        val c = state(Block(BlockType.Paragraph, "")).blocks[0]
        val s2 = state(Block(BlockType.Paragraph, ""))
        s2.onValueChange(s2.blocks[0], c.type("[] "))
        assertEquals(BlockType.Todo, s2.blocks[0].type)
    }

    @Test
    fun threeDashesBecomeADividerAndMoveOn() {
        val s = state(Block(BlockType.Paragraph, "--"))
        val b = s.blocks[0]
        s.onValueChange(b, b.type("---"))
        assertEquals(listOf(BlockType.Divider, BlockType.Paragraph), s.types())
    }

    @Test
    fun backspaceAtTheStartPeelsOffFormattingThenJoinsBlocks() {
        val s = state(Block(BlockType.Paragraph, "one"), Block(BlockType.Quote, "two"))
        val quote = s.blocks[1]
        assertTrue(s.backspaceAtStart(quote))
        assertEquals(BlockType.Paragraph, quote.type)
        assertTrue(s.backspaceAtStart(quote))
        assertEquals("onetwo", s.blocks[0].text)
        assertEquals(1 + 1, s.blocks.size) // joined text plus the trailing paragraph
    }

    @Test
    fun backspaceAfterADividerRemovesIt() {
        val s = state(Block(BlockType.Divider), Block(BlockType.Paragraph, "after"))
        assertTrue(s.backspaceAtStart(s.blocks[1]))
        assertEquals(listOf(BlockType.Paragraph), s.types())
    }

    @Test
    fun theFirstBlockHasNothingToJoinInto() {
        val s = state(Block(BlockType.Paragraph, "only"))
        assertFalse(s.backspaceAtStart(s.blocks[0]))
    }

    @Test
    fun slashCommandsConvertTheBlockAndDropTheSlashText() {
        val s = state(Block(BlockType.Paragraph, ""))
        val b = s.blocks[0]
        b.value = TextFieldValue("/head", TextRange(5))
        val token = s.slashToken(b.also { s.focusedId = it.id })!!
        s.applySlash(b, token, SlashCommand.filter(token.query).first())
        assertEquals(BlockType.Heading1, b.type)
        assertEquals("", b.text)
    }

    @Test
    fun slashInsideTextKeepsTheText() {
        val s = state(Block(BlockType.Paragraph, "buy milk /todo"))
        val b = s.blocks[0]
        b.value = TextFieldValue(b.text, TextRange(b.text.length))
        s.focusedId = b.id
        val token = s.slashToken(b)!!
        s.applySlash(b, token, SlashCommand.Todo)
        assertEquals(BlockType.Todo, b.type)
        assertEquals("buy milk ", b.text)
    }

    @Test
    fun inlineSlashCommandsInsertAMarkerPairAndPlaceTheCaretBetween() {
        val s = state(Block(BlockType.Paragraph, "/bold"))
        val b = s.blocks[0]
        b.value = TextFieldValue("/bold", TextRange(5))
        s.focusedId = b.id
        val token = s.slashToken(b)!!
        s.applySlash(b, token, SlashCommand.Bold)
        assertEquals("****", b.text)
        assertEquals(TextRange(2), b.value.selection)
    }

    @Test
    fun enterWhileTheSlashMenuIsOpenPicksTheFirstMatch() {
        val s = state(Block(BlockType.Paragraph, ""))
        val b = s.blocks[0]
        b.value = TextFieldValue("/quote", TextRange(6))
        s.focusedId = b.id
        s.onValueChange(b, TextFieldValue("/quote\n", TextRange(7)))
        assertEquals(BlockType.Quote, b.type)
        assertEquals(2, s.blocks.size) // no new block was started
    }

    @Test
    fun aDismissedSlashStaysClosed() {
        val s = state(Block(BlockType.Paragraph, "/"))
        val b = s.blocks[0]
        b.value = TextFieldValue("/", TextRange(1))
        s.focusedId = b.id
        val token = s.slashToken(b)!!
        s.slashDismissed = b.id to token.from
        assertEquals(null, s.slashToken(b))
    }

    @Test
    fun pastedMarkdownBecomesBlocks() {
        val s = state(Block(BlockType.Paragraph, ""))
        val b = s.blocks[0]
        s.onValueChange(b, b.type("# Plan\n- one\n- two"))
        assertEquals(listOf(BlockType.Heading1, BlockType.Bullet, BlockType.Bullet, BlockType.Paragraph), s.types())
    }

    @Test
    fun indentOnlyAppliesToListsAndStopsAtThreeLevels() {
        val s = state(Block(BlockType.Bullet, "a"), Block(BlockType.Paragraph, "p"))
        repeat(6) { s.indent(s.blocks[0], 1) }
        assertEquals(3, s.blocks[0].indent)
        s.indent(s.blocks[1], 1)
        assertEquals(0, s.blocks[1].indent)
        repeat(6) { s.indent(s.blocks[0], -1) }
        assertEquals(0, s.blocks[0].indent)
    }

    @Test
    fun codeBlocksKeepNewlinesAndLeaveOnADoubleEnter() {
        val s = state(Block(BlockType.Code, "a"))
        val c = s.blocks[0]
        s.onValueChange(c, c.type("a\n"))
        assertEquals("a\n", c.text)
        s.onValueChange(c, c.type("a\n\n"))
        assertEquals("a", c.text)
        assertEquals(BlockType.Paragraph, s.blocks[1].type)
    }

    @Test
    fun theEditorRoundTripsToMarkdown() {
        val source = "# Plan\n\n- one\n- two\n\nDone."
        val s = EditorState(com.yashraj.cadence.domain.Markdown.parse(source))
        assertEquals(source, com.yashraj.cadence.domain.Markdown.serialize(s.toBlocks()))
    }
}
