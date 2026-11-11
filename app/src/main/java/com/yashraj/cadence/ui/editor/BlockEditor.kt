package com.yashraj.cadence.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yashraj.cadence.domain.Block
import com.yashraj.cadence.domain.BlockType
import com.yashraj.cadence.domain.Inline
import com.yashraj.cadence.domain.InlineKind
import com.yashraj.cadence.domain.InlineSpan
import com.yashraj.cadence.domain.Markdown
import com.yashraj.cadence.ui.components.CCheck
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.CheckShape
import com.yashraj.cadence.ui.components.CheckState
import com.yashraj.cadence.ui.components.tappable
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceColors
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Radius

private fun styleFor(kind: InlineKind, colors: CadenceColors) = when (kind) {
    InlineKind.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    InlineKind.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    InlineKind.Strike -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    InlineKind.Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = colors.track, fontSize = 0.92.em)
    InlineKind.Link -> SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline)
}

/**
 * Draws inline Markdown over the raw text. While a block has the caret its markers stay visible but dimmed, so
 * editing is honest; when it does not, the markers are hidden and only the formatting shows.
 */
private class InlineStyler(private val colors: CadenceColors, private val hideMarkers: Boolean) : VisualTransformation {
    var spans: List<InlineSpan> = emptyList()
        private set
    var mapping: OffsetMapping = OffsetMapping.Identity
        private set

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val n = raw.length
        val found = Inline.spans(raw)
        spans = found
        if (found.isEmpty()) {
            mapping = OffsetMapping.Identity
            return TransformedText(text, OffsetMapping.Identity)
        }
        if (!hideMarkers) {
            val marker = SpanStyle(color = colors.inkMuted.copy(alpha = 0.7f))
            val builder = AnnotatedString.Builder(raw)
            found.forEach { s ->
                if (s.contentEnd > s.contentStart) builder.addStyle(styleFor(s.kind, colors), s.contentStart, s.contentEnd)
                builder.addStyle(marker, s.openStart, s.openEnd)
                if (s.closeEnd > s.closeStart) builder.addStyle(marker, s.closeStart, s.closeEnd)
            }
            mapping = OffsetMapping.Identity
            return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
        val hidden = Inline.hidden(n, found)
        val toDisplay = IntArray(n + 1)
        val toRaw = ArrayList<Int>(n + 1)
        val display = StringBuilder()
        for (i in 0 until n) {
            toDisplay[i] = display.length
            if (!hidden[i]) {
                toRaw += i
                display.append(raw[i])
            }
        }
        toDisplay[n] = display.length
        toRaw += n
        val builder = AnnotatedString.Builder(display.toString())
        found.forEach { s ->
            val a = toDisplay[s.contentStart]
            val e = toDisplay[s.contentEnd]
            if (e > a) builder.addStyle(styleFor(s.kind, colors), a, e)
        }
        val map = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toDisplay[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int) = toRaw[offset.coerceIn(0, toRaw.size - 1)]
        }
        mapping = map
        return TransformedText(builder.toAnnotatedString(), map)
    }
}

@Composable
private fun textStyleFor(type: BlockType, checked: Boolean, colors: CadenceColors): TextStyle {
    val base = when (type) {
        BlockType.Heading1 -> CadenceType.title.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight(700))
        BlockType.Heading2 -> CadenceType.title.copy(fontSize = 21.sp, lineHeight = 27.sp)
        BlockType.Heading3 -> CadenceType.heading
        BlockType.Code -> CadenceType.callout.copy(fontFamily = FontFamily.Monospace, lineHeight = 21.sp)
        else -> CadenceType.body
    }
    return when {
        type == BlockType.Todo && checked -> base.copy(color = colors.inkMuted, textDecoration = TextDecoration.LineThrough)
        type == BlockType.Quote -> base.copy(color = colors.inkMuted)
        else -> base.copy(color = colors.ink)
    }
}

private fun placeholderFor(type: BlockType, focused: Boolean, last: Boolean) = when (type) {
    BlockType.Paragraph -> if (focused) "Type / for formatting" else if (last) "Keep writing…" else ""
    BlockType.Heading1 -> "Heading 1"
    BlockType.Heading2 -> "Heading 2"
    BlockType.Heading3 -> "Heading 3"
    BlockType.Bullet, BlockType.Numbered -> "List item"
    BlockType.Todo -> "To-do"
    BlockType.Quote -> "Quote"
    BlockType.Code -> "Code"
    BlockType.Divider -> ""
}

private fun topPadding(type: BlockType, first: Boolean) = if (first) 0.dp else when (type) {
    BlockType.Heading1 -> 14.dp
    BlockType.Heading2 -> 10.dp
    BlockType.Heading3 -> 6.dp
    else -> 4.dp
}

private fun openLink(uriHandler: androidx.compose.ui.platform.UriHandler, url: String) {
    val target = if (url.contains("://") || url.startsWith("mailto:")) url else "https://$url"
    runCatching { uriHandler.openUri(target) }
}

/** The editable blocks of a note. */
@Composable
fun BlockEditor(state: EditorState, modifier: Modifier = Modifier) {
    val colors = Cadence.colors
    val shape = state.blocks.map { Block(it.type, indent = it.indent) }
    val numbers = Markdown.numbering(shape)
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(colors.accent, colors.accent.copy(alpha = 0.28f)),
    ) {
        Column(modifier.fillMaxWidth()) {
            state.blocks.forEachIndexed { index, block ->
                key(block.id) { BlockRow(state, block, numbers[index], index == 0, index == state.blocks.lastIndex) }
            }
        }
    }
}

@Composable
private fun BlockRow(state: EditorState, block: BlockState, number: Int, first: Boolean, last: Boolean) {
    val colors = Cadence.colors
    val type = block.type
    if (type == BlockType.Divider) {
        DividerRow(state, block)
        return
    }
    val uriHandler = LocalUriHandler.current
    val hasFocus = block.hasFocus
    val styler = remember(colors, hasFocus) { InlineStyler(colors, hideMarkers = !hasFocus) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textStyle = textStyleFor(type, block.checked, colors)

    val pending = state.pendingFocus
    LaunchedEffect(pending) {
        if (pending != null && pending.id == block.id) {
            runCatching { block.focus.requestFocus() }
            if (!pending.keepSelection) {
                val caret = (pending.cursor ?: block.text.length).coerceIn(0, block.text.length)
                block.value = block.value.copy(selection = TextRange(caret))
            }
            state.pendingFocus = null
        }
    }

    val field: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            val placeholder = placeholderFor(type, hasFocus, last)
            if (block.text.isEmpty() && placeholder.isNotEmpty()) {
                CText(placeholder, textStyle.copy(textDecoration = null), color = colors.inkMuted, maxLines = 1)
            }
            BasicTextField(
                value = block.value,
                onValueChange = { state.onValueChange(block, it) },
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.accent),
                visualTransformation = styler,
                onTextLayout = { layout = it },
                keyboardOptions = KeyboardOptions(capitalization = if (type == BlockType.Code) KeyboardCapitalization.None else KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(block.focus)
                    .onFocusChanged {
                        block.hasFocus = it.isFocused
                        if (it.isFocused) {
                            state.focusedId = block.id
                            state.selectedDividerId = -1
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.Backspace && block.value.selection.collapsed && block.value.selection.start == 0 ->
                                state.backspaceAtStart(block)
                            event.isCtrlPressed && event.key == Key.B -> { state.toggleInline(block, "**"); true }
                            event.isCtrlPressed && event.key == Key.I -> { state.toggleInline(block, "*"); true }
                            event.isCtrlPressed && event.key == Key.K -> { state.insertLink(block); true }
                            else -> false
                        }
                    }
                    // A tap on a link while the block is not being edited opens it, instead of moving the caret.
                    .pointerInput(hasFocus) {
                        if (hasFocus) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val up = waitForUpOrCancellation(PointerEventPass.Initial)
                            val l = layout
                            if (up != null && l != null) {
                                val raw = styler.mapping.transformedToOriginal(l.getOffsetForPosition(down.position))
                                val link = styler.spans.firstOrNull { it.kind == InlineKind.Link && raw in it.contentStart..it.contentEnd }
                                if (link?.url != null) {
                                    up.consume()
                                    openLink(uriHandler, link.url)
                                }
                            }
                        }
                    },
            )
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = (block.indent * 24).dp, top = topPadding(type, first))
            .then(if (type == BlockType.Quote) Modifier.height(IntrinsicSize.Min) else Modifier),
        verticalAlignment = Alignment.Top,
    ) {
        when (type) {
            BlockType.Bullet -> CText("•", CadenceType.body, Modifier.width(24.dp), color = colors.ink, maxLines = 1)
            BlockType.Numbered -> CText("$number.", CadenceType.body, Modifier.width(28.dp), color = colors.inkMuted, maxLines = 1)
            BlockType.Todo -> Box(Modifier.size(width = 36.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(48.dp), contentAlignment = Alignment.Center) {
                    CCheck(
                        if (block.checked) CheckState.Done else CheckState.Off,
                        "Tick ${block.text.ifBlank { "item" }}",
                        { block.checked = !block.checked },
                        shape = CheckShape.Square,
                    )
                }
            }
            BlockType.Quote -> Box(
                Modifier
                    .padding(end = 12.dp)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(colors.accent, RoundedCornerShape(2.dp)),
            )
            else -> Unit
        }
        if (type == BlockType.Code) {
            Box(
                Modifier
                    .weight(1f)
                    .background(colors.track, RoundedCornerShape(Radius.tag))
                    .padding(12.dp),
            ) { field() }
        } else {
            Box(Modifier.weight(1f).padding(top = if (type == BlockType.Todo) 4.dp else 0.dp)) { field() }
        }
    }
}

@Composable
private fun DividerRow(state: EditorState, block: BlockState) {
    val colors = Cadence.colors
    val selected = state.selectedDividerId == block.id
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .tappable(
                { state.selectedDividerId = if (selected) -1 else block.id },
                onClickLabel = "Select divider",
                role = Role.Button,
                pressScale = 1f,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(if (colors.highContrast) colors.controlEdge else colors.controlEdge.copy(alpha = 0.45f)),
        )
        if (selected) {
            CIconButton(CadenceIcons.Trash, "Remove divider", { state.removeDivider(block) }, glass = false, tint = colors.danger)
        }
    }
}
