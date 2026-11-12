package com.yashraj.cadence.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashraj.cadence.data.NoteEntity
import com.yashraj.cadence.data.TaskEntity
import com.yashraj.cadence.domain.BlockType
import com.yashraj.cadence.domain.Dates
import com.yashraj.cadence.domain.Markdown
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.components.ActionMenu
import com.yashraj.cadence.ui.components.CChip
import com.yashraj.cadence.ui.components.CField
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.CTag
import com.yashraj.cadence.ui.components.MenuItem
import com.yashraj.cadence.ui.components.SceneryHost
import com.yashraj.cadence.ui.components.glass
import com.yashraj.cadence.ui.editor.BlockEditor
import com.yashraj.cadence.ui.editor.EditorState
import com.yashraj.cadence.ui.editor.FormatAction
import com.yashraj.cadence.ui.editor.FormatBar
import com.yashraj.cadence.ui.editor.SlashMenu
import com.yashraj.cadence.ui.theme.CText
import com.yashraj.cadence.ui.theme.Cadence
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.CadenceType
import com.yashraj.cadence.ui.theme.Radius
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private data class Snapshot(val title: String, val tag: String?, val pinned: Boolean, val body: String)

/**
 * The note editor: a block-based Markdown editor on one full-width raised tile, so body text has the steadiest
 * ground. Type "/" for formatting, or use Markdown shortcuts like "# " and "- ". It saves itself as you type, and
 * the note is stored as plain Markdown that can be viewed, copied and exported.
 */
@OptIn(FlowPreview::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(vm: AppViewModel, noteId: Long?, onBack: () -> Unit) {
    val colors = Cadence.colors
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val notes by vm.notes.collectAsState()
    val today by vm.today.collectAsState()
    val initial = remember { noteId?.let { id -> vm.notes.value?.firstOrNull { it.id == id } } }

    var id by remember { mutableLongStateOf(initial?.id ?: 0L) }
    var title by remember { mutableStateOf(TextFieldValue(initial?.title ?: "")) }
    var tag by remember { mutableStateOf(initial?.tag) }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }
    val state = remember { EditorState(Markdown.parse(initial?.body.orEmpty())) }
    // Non-null while the raw Markdown source is showing instead of the blocks.
    var raw by remember { mutableStateOf<TextFieldValue?>(null) }
    var saved by remember { mutableStateOf(if (initial != null) "Saved" else "") }
    var tagPicker by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    fun body(): String = raw?.text ?: Markdown.serialize(state.toBlocks())
    fun snapshot() = Snapshot(title.text.trim(), tag, pinned, body())
    var lastSaved by remember { mutableStateOf(snapshot()) }

    suspend fun persist(s: Snapshot) {
        if (id == 0L && s.title.isBlank() && s.body.isBlank()) return
        id = vm.saveNote(NoteEntity(id = id, title = s.title, body = s.body, tag = s.tag, pinned = s.pinned, updatedAt = Dates.nowMillis()))
        lastSaved = s
        saved = "Saved just now"
    }

    LaunchedEffect(Unit) {
        snapshotFlow { snapshot() }.drop(1).debounce(500).collect { persist(it) }
    }
    DisposableEffect(Unit) {
        onDispose {
            val s = snapshot()
            if (s != lastSaved && !(id == 0L && s.title.isBlank() && s.body.isBlank())) {
                vm.saveNoteAsync(NoteEntity(id = id, title = s.title, body = s.body, tag = s.tag, pinned = s.pinned, updatedAt = Dates.nowMillis()))
            }
        }
    }
    LaunchedEffect(Unit) {
        if (initial == null) titleFocus.requestFocus()
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) scope.launch {
            val s = snapshot()
            val text = (if (s.title.isNotBlank()) "# ${s.title}\n\n" else "") + s.body + "\n"
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            vm.showToast("Exported as Markdown")
        }
    }

    // The slash menu: open while a "/command" is being typed in the block that has the caret.
    val focusedBlock = if (raw == null) state.focused else null
    val token = state.slashToken(focusedBlock)
    val commands = if (token != null) com.yashraj.cadence.domain.SlashCommand.filter(token.query) else emptyList()
    val menuVisible = focusedBlock != null && token != null && commands.isNotEmpty()
    BackHandler(enabled = menuVisible) { state.slashDismissed = focusedBlock!!.id to token!!.from }

    val checklist = if (raw == null) Markdown.progress(state.toBlocks()) else null

    SceneryHost {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = Space.s4, start = Space.s5, end = Space.s5),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = Space.s4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CIconButton(CadenceIcons.Back, "Back to notes", onBack)
                CText(saved, EyebrowStyle, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    CIconButton(CadenceIcons.Pin, if (pinned) "Unpin note" else "Pin note", { pinned = !pinned }, selected = pinned)
                    Box {
                        CIconButton(CadenceIcons.More, "More options", { menuOpen = true })
                        ActionMenu(
                            expanded = menuOpen,
                            onDismiss = { menuOpen = false },
                            items = listOf(
                                if (raw == null) {
                                    MenuItem("Markdown source", CadenceIcons.Code) {
                                        focusManager.clearFocus()
                                        raw = TextFieldValue(Markdown.serialize(state.toBlocks()))
                                    }
                                } else {
                                    MenuItem("Back to editor", CadenceIcons.Notes) {
                                        state.load(Markdown.parse(raw!!.text))
                                        raw = null
                                    }
                                },
                                MenuItem("Copy Markdown", CadenceIcons.Copy) {
                                    clipboard.setText(AnnotatedString(body()))
                                    vm.showToast("Markdown copied")
                                },
                                MenuItem("Export as .md", CadenceIcons.Download) {
                                    val name = title.text.trim().ifBlank { "note" }.replace(Regex("""[^\w\- ]"""), "").trim().ifBlank { "note" }
                                    exportLauncher.launch("$name.md")
                                },
                                MenuItem("Delete note", CadenceIcons.Trash, danger = true) {
                                    val s = snapshot()
                                    if (id != 0L) {
                                        vm.deleteNote(NoteEntity(id = id, title = s.title, body = s.body, tag = s.tag, pinned = s.pinned, updatedAt = Dates.nowMillis()))
                                    }
                                    lastSaved = s // nothing left to flush
                                    onBack()
                                },
                            ),
                        )
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet), raised = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.s5, vertical = Space.s6)
                    .padding(bottom = if (menuVisible) 380.dp else 96.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                Box {
                    if (title.text.isEmpty()) CText("Title", CadenceType.title, color = colors.inkMuted)
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it.copy(text = it.text.replace("\n", " ")) },
                        textStyle = CadenceType.title.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.accent),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { state.blocks.firstOrNull()?.let { state.focus(it) } }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocus)
                            .onFocusChanged { if (it.isFocused) state.focusedId = -1 }
                            .semantics { contentDescription = "Note title" },
                    )
                }
                if (tag != null || checklist != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalAlignment = Alignment.CenterVertically) {
                        tag?.let { CTag(it) }
                        checklist?.let { CText("${it.first} of ${it.second} done", CadenceType.caption, color = colors.inkMuted) }
                    }
                }
                if (tagPicker) Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    val existing = (notes?.mapNotNull { it.tag }.orEmpty() + listOfNotNull(tag)).distinct().sorted()
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                        existing.forEach { name -> CChip(name, tag == name, { tag = name; tagPicker = false }) }
                        CChip("None", tag == null, { tag = null; tagPicker = false })
                    }
                    CField(
                        value = newTag,
                        onValueChange = { newTag = it.take(20) },
                        placeholder = "New tag",
                        label = "New tag",
                        minHeight = 48.dp,
                        textStyle = CadenceType.body,
                        onImeAction = {
                            if (newTag.isNotBlank()) {
                                tag = newTag.trim()
                                newTag = ""
                                tagPicker = false
                            }
                        },
                    )
                }

                val source = raw
                if (source != null) {
                    // Markdown source: the same note as plain text, for anyone who prefers to type it all.
                    Box(Modifier.fillMaxWidth().heightIn(min = 240.dp)) {
                        BasicTextField(
                            value = source,
                            onValueChange = { raw = it },
                            textStyle = CadenceType.callout.copy(color = colors.ink, fontFamily = FontFamily.Monospace, lineHeight = 21.sp),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Markdown source" },
                        )
                    }
                } else {
                    BlockEditor(state)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.tapBelow() },
                    )
                }
            }
        }

        val insets = WindowInsets.navigationBars.union(WindowInsets.ime)
        if (menuVisible) {
            SlashMenu(
                commands = commands,
                onPick = { command -> state.applySlash(focusedBlock!!, token!!, command) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(insets)
                    .padding(start = Space.s4, end = Space.s4, bottom = TabBarBottomGap + 56.dp + Space.s2),
            )
        }

        val block = focusedBlock
        val formatting = raw == null
        FormatBar(
            actions = if (!formatting) emptyList() else listOf(
                FormatAction(CadenceIcons.Slash, "Formatting menu", { block?.let(state::insertSlash) ?: state.tapBelow() }),
                FormatAction(null, ""),
                FormatAction(CadenceIcons.Bold, "Bold", { block?.let { state.toggleInline(it, "**") } }),
                FormatAction(CadenceIcons.Italic, "Italic", { block?.let { state.toggleInline(it, "*") } }),
                FormatAction(CadenceIcons.Strike, "Strikethrough", { block?.let { state.toggleInline(it, "~~") } }),
                FormatAction(CadenceIcons.Code, "Inline code", { block?.let { state.toggleInline(it, "`") } }),
                FormatAction(CadenceIcons.Link, "Link", { block?.let(state::insertLink) }),
                FormatAction(null, ""),
                FormatAction(CadenceIcons.BulletList, "Bulleted list", { block?.let { state.toggleType(it, BlockType.Bullet) } }, block?.type == BlockType.Bullet),
                FormatAction(CadenceIcons.ListChecks, "To-do list", { block?.let { state.toggleType(it, BlockType.Todo) } }, block?.type == BlockType.Todo),
                FormatAction(CadenceIcons.Outdent, "Outdent", { block?.let { state.indent(it, -1) } }),
                FormatAction(CadenceIcons.Indent, "Indent", { block?.let { state.indent(it, 1) } }),
                FormatAction(null, ""),
                FormatAction(CadenceIcons.Tag, "Add tag", { tagPicker = !tagPicker }, tagPicker),
                FormatAction(CadenceIcons.Tasks, "Turn into a task", {
                    val s = snapshot()
                    val text = s.title.ifBlank { state.blocks.firstOrNull { it.text.isNotBlank() }?.text.orEmpty() }
                    if (text.isNotBlank()) vm.addTask(TaskEntity(title = text.trim(), dueDay = today, tag = s.tag, createdAt = 0))
                }),
            ),
            onDone = {
                focusManager.clearFocus()
                onBack()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(insets)
                .padding(start = Space.s4, end = Space.s4, bottom = TabBarBottomGap),
        )
    }
}
