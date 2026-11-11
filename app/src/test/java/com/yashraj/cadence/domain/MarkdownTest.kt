package com.yashraj.cadence.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {
    @Test
    fun parsesEveryBlockKind() {
        val source = """
            # Title
            ## Section
            ### Small

            A paragraph with **bold**.

            - one
            - two
              - nested
            1. first
            2. second
            - [ ] open
            - [x] done

            > quoted

            ---

            ```
            val a = 1
            val b = 2
            ```
        """.trimIndent()
        val blocks = Markdown.parse(source)
        assertEquals(
            listOf(
                BlockType.Heading1, BlockType.Heading2, BlockType.Heading3, BlockType.Paragraph,
                BlockType.Bullet, BlockType.Bullet, BlockType.Bullet, BlockType.Numbered, BlockType.Numbered,
                BlockType.Todo, BlockType.Todo, BlockType.Quote, BlockType.Divider, BlockType.Code,
            ),
            blocks.map { it.type },
        )
        assertEquals(1, blocks[6].indent)
        assertEquals(listOf(false, true), blocks.filter { it.type == BlockType.Todo }.map { it.checked })
        assertEquals("val a = 1\nval b = 2", blocks.last().text)
    }

    @Test
    fun serializeThenParseKeepsTheBlocks() {
        val blocks = listOf(
            Block(BlockType.Heading1, "Trip"),
            Block(BlockType.Paragraph, "Pack the *night* before."),
            Block(BlockType.Todo, "Passport", checked = true),
            Block(BlockType.Todo, "Charger"),
            Block(BlockType.Bullet, "Shoes"),
            Block(BlockType.Bullet, "Trainers", indent = 1),
            Block(BlockType.Numbered, "Leave at six"),
            Block(BlockType.Numbered, "Lock up"),
            Block(BlockType.Quote, "Travel light"),
            Block(BlockType.Divider),
            Block(BlockType.Code, "echo hi"),
        )
        assertEquals(blocks, Markdown.parse(Markdown.serialize(blocks)))
    }

    @Test
    fun consecutiveListItemsSitOnConsecutiveLinesAndOtherBlocksAreSeparatedByBlankLines() {
        val text = Markdown.serialize(
            listOf(Block(BlockType.Paragraph, "Intro"), Block(BlockType.Bullet, "a"), Block(BlockType.Bullet, "b"), Block(BlockType.Paragraph, "Outro")),
        )
        assertEquals("Intro\n\n- a\n- b\n\nOutro", text)
    }

    @Test
    fun emptyParagraphsAreNotSaved() {
        assertEquals("Hello", Markdown.serialize(listOf(Block(BlockType.Paragraph, "Hello"), Block(BlockType.Paragraph, ""))))
        assertEquals("", Markdown.serialize(listOf(Block(BlockType.Paragraph, ""))))
    }

    @Test
    fun numberedListsCountPerLevelAndRestartAfterOtherBlocks() {
        val blocks = listOf(
            Block(BlockType.Numbered, "a"),
            Block(BlockType.Numbered, "b"),
            Block(BlockType.Numbered, "b1", indent = 1),
            Block(BlockType.Numbered, "b2", indent = 1),
            Block(BlockType.Numbered, "c"),
            Block(BlockType.Paragraph, "break"),
            Block(BlockType.Numbered, "again"),
        )
        assertEquals(listOf(1, 2, 1, 2, 3, 0, 1), Markdown.numbering(blocks).toList())
        assertTrue(Markdown.serialize(blocks).contains("  1. b1"))
    }

    @Test
    fun readsNotesWrittenByTheEarlierChecklistEditor() {
        val old = "- [x] Passport and ID\n- [ ] Charger\nCheck the forecast."
        val blocks = Markdown.parse(old)
        assertEquals(listOf(BlockType.Todo, BlockType.Todo, BlockType.Paragraph), blocks.map { it.type })
        assertEquals(1 to 2, Markdown.progress(blocks))
    }

    @Test
    fun previewCountsDoneItemsAndReadsPlainText() {
        val body = "- [x] Passport and ID\n- [ ] Charger and the **long** cable\nCheck the monsoon forecast."
        assertEquals("1 of 2 done · Charger and the long cable. Check the monsoon forecast.", Markdown.preview(body))
        assertEquals("Open with the chai story.", Markdown.preview("Open with the chai story."))
        assertEquals("", Markdown.preview(""))
    }

    @Test
    fun parsesInlineEmphasisCodeAndLinks() {
        val text = "a **bold** and *italic* and ~~gone~~ and `code` and [site](https://x.dev)"
        val kinds = Inline.spans(text).map { it.kind }
        assertEquals(setOf(InlineKind.Bold, InlineKind.Italic, InlineKind.Strike, InlineKind.Code, InlineKind.Link), kinds.toSet())
        assertEquals("a bold and italic and gone and code and site", Inline.strip(text))
        val link = Inline.spans(text).first { it.kind == InlineKind.Link }
        assertEquals("https://x.dev", link.url)
        assertEquals("site", text.substring(link.contentStart, link.contentEnd))
    }

    @Test
    fun codeAndLinkTargetsAreLeftAlone() {
        assertEquals("keep **this** literal", Inline.strip("`keep **this** literal`"))
        assertEquals(listOf(InlineKind.Link), Inline.spans("[a_b_c](http://x_y_z)").map { it.kind })
        assertEquals("snake_case_name", Inline.strip("snake_case_name"))
        assertEquals("2 * 3 * 4", Inline.strip("2 * 3 * 4"))
    }

    @Test
    fun emphasisNestsInsideEmphasis() {
        val spans = Inline.spans("**bold *and italic* text**")
        assertEquals(setOf(InlineKind.Bold, InlineKind.Italic), spans.map { it.kind }.toSet())
        assertEquals("bold and italic text", Inline.strip("**bold *and italic* text**"))
    }
}

class EditorOpsTest {
    @Test
    fun markdownPrefixesBecomeBlocks() {
        assertEquals(BlockType.Heading1, EditorOps.shortcut("# Hi")!!.type)
        assertEquals("Hi", EditorOps.shortcut("## Hi")!!.rest)
        assertEquals(BlockType.Heading3, EditorOps.shortcut("### Hi")!!.type)
        assertEquals(BlockType.Bullet, EditorOps.shortcut("- item")!!.type)
        assertEquals(BlockType.Bullet, EditorOps.shortcut("* item")!!.type)
        assertEquals(BlockType.Numbered, EditorOps.shortcut("1. item")!!.type)
        assertEquals("item", EditorOps.shortcut("12) item")!!.rest)
        assertEquals(BlockType.Quote, EditorOps.shortcut("> wise")!!.type)
        assertEquals(BlockType.Divider, EditorOps.shortcut("---")!!.type)
        assertEquals(BlockType.Code, EditorOps.shortcut("```")!!.type)
        val todo = EditorOps.shortcut("[] buy milk")!!
        assertEquals(BlockType.Todo, todo.type)
        assertEquals(false, todo.checked)
        assertEquals(true, EditorOps.shortcut("[x] done")!!.checked)
        assertNull(EditorOps.shortcut("plain text"))
        assertNull(EditorOps.shortcut("#hashtag"))
    }

    @Test
    fun slashOpensAtTheStartOrAfterASpaceOnly() {
        assertEquals(SlashToken(0, 1, ""), EditorOps.slash("/", 1))
        assertEquals(SlashToken(0, 4, "hea"), EditorOps.slash("/hea", 4))
        assertEquals(SlashToken(6, 8, "b"), EditorOps.slash("hello /b", 8).let { it?.copy(to = 8) })
        assertNull(EditorOps.slash("http://x", 8))
        assertNull(EditorOps.slash("and/or", 6))
        assertNull(EditorOps.slash("/ space", 7))
        assertNull(EditorOps.slash("no slash", 8))
    }

    @Test
    fun slashFiltersAndRanksCommands() {
        assertEquals(SlashCommand.entries, SlashCommand.filter(""))
        assertEquals(SlashCommand.Heading1, SlashCommand.filter("h1").first())
        assertEquals(SlashCommand.Todo, SlashCommand.filter("todo").first())
        assertEquals(SlashCommand.Bold, SlashCommand.filter("bol").first())
        assertTrue(SlashCommand.filter("zzzz").isEmpty())
        // Names that start with the query come before looser keyword matches.
        assertEquals(SlashCommand.Code, SlashCommand.filter("code").first())
    }

    @Test
    fun togglingInlineWrapsAndUnwraps() {
        val wrapped = EditorOps.toggleInline(Sel("make this bold", 5, 9), "**")
        assertEquals(Sel("make **this** bold", 7, 11), wrapped)
        assertEquals(Sel("make this bold", 5, 9), EditorOps.toggleInline(wrapped, "**"))
        // Selecting the markers too also unwraps.
        assertEquals(Sel("make this bold", 5, 9), EditorOps.toggleInline(Sel("make **this** bold", 5, 13), "**"))
        // A caret inserts an empty pair with the caret between.
        assertEquals(Sel("ab****cd", 4, 4), EditorOps.toggleInline(Sel("abcd", 2, 2), "**"))
    }

    @Test
    fun italicIsNotConfusedWithBold() {
        val bold = Sel("**word**", 2, 6)
        val result = EditorOps.toggleInline(bold, "*")
        assertEquals("***word***", result.text)
    }

    @Test
    fun linkPutsTheCaretWhereTheAddressGoes() {
        assertEquals(Sel("see [docs]() now", 11, 11), EditorOps.link(Sel("see docs now", 4, 8)))
        assertEquals(Sel("[]()", 1, 1), EditorOps.link(Sel("", 0, 0)))
    }
}
