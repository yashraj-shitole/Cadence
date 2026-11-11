package com.yashraj.cadence.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashraj.cadence.ui.AppViewModel
import com.yashraj.cadence.ui.components.CChip
import com.yashraj.cadence.ui.components.CIconButton
import com.yashraj.cadence.ui.components.EmptyState
import com.yashraj.cadence.ui.components.Glows
import com.yashraj.cadence.ui.components.NoteCard
import com.yashraj.cadence.ui.components.SearchField
import com.yashraj.cadence.ui.components.SectionHeading
import com.yashraj.cadence.ui.theme.CadenceIcons
import com.yashraj.cadence.ui.theme.Space
import kotlinx.coroutines.delay

/** Notes: search, tag chips, then Pinned and All notes. */
@Composable
fun NotesScreen(vm: AppViewModel, onOpen: (Long?) -> Unit) {
    val notes by vm.notes.collectAsState()
    val today by vm.today.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<String?>(null) }
    var intro by remember { mutableStateOf(true) }
    LaunchedEffect(notes != null) {
        if (notes != null) {
            delay(700)
            intro = false
        }
    }

    ScreenFrame(Glows.Notes) {
        val list = notes
        ScreenHeader(
            title = "Notes",
            eyebrow = list?.let { "${it.size} ${if (it.size == 1) "note" else "notes"} · ${it.count { n -> n.pinned }} pinned" },
            trailing = { CIconButton(CadenceIcons.Plus, "New note", { onOpen(null) }) },
        )
        if (list == null) return@ScreenFrame
        if (list.isEmpty()) {
            EmptyState("No notes yet. Write the first line and it saves itself.", "New note", { onOpen(null) })
            return@ScreenFrame
        }
        SearchField(query, { query = it }, "Search notes")
        val tags = list.mapNotNull { it.tag }.distinct().sorted()
        if (filter != null && filter !in tags) filter = null
        if (tags.isNotEmpty()) {
            Row(
                Modifier
                    .padding(top = Space.s2)
                    .bleedHorizontal(Space.s5)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space.s5),
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                CChip("All", filter == null, { filter = null })
                tags.forEach { tag -> CChip(tag, filter == tag, { filter = tag }) }
            }
        }
        val q = query.trim()
        val shown = list.filter { n ->
            (filter == null || n.tag == filter) &&
                (q.isEmpty() || n.title.contains(q, ignoreCase = true) || n.body.contains(q, ignoreCase = true))
        }
        val pinned = shown.filter { it.pinned }
        val rest = shown.filter { !it.pinned }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = listPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            if (pinned.isNotEmpty()) {
                item(key = "pinned") { SectionHeading("Pinned", trailing = pinned.size.toString()) }
                items(pinned, key = { it.id }) { note ->
                    NoteCard(note, today, { onOpen(note.id) }, Modifier.riseIn(shown.indexOf(note), intro))
                }
            }
            if (rest.isNotEmpty()) {
                item(key = "all") { SectionHeading("All notes", trailing = rest.size.toString()) }
                items(rest, key = { it.id }) { note ->
                    NoteCard(note, today, { onOpen(note.id) }, Modifier.riseIn(shown.indexOf(note), intro))
                }
            }
            if (shown.isEmpty()) {
                item(key = "none") { EmptyState("No notes match that.", "Clear search", { query = ""; filter = null }) }
            }
        }
    }
}
