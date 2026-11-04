(() => {
'use strict';

// ---------- storage ----------
const KEY = 'cadence.v1';
const blank = () => ({ habits: [], todos: [], notes: [] });
let state = load();

function load() {
  try {
    const s = JSON.parse(localStorage.getItem(KEY));
    return s && typeof s === 'object' ? { ...blank(), ...s } : blank();
  } catch { return blank(); }
}
function save() {
  try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) { console.warn('Could not save', e); }
}
const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
const $ = (s) => document.querySelector(s);
const el = (tag, props = {}, ...kids) => {
  const n = Object.assign(document.createElement(tag), props);
  kids.forEach((k) => n.append(k));
  return n;
};

// ---------- dates (local time) ----------
const pad = (n) => String(n).padStart(2, '0');
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
const todayStr = () => fmt(new Date());

// ---------- tabs ----------
document.querySelectorAll('.tabs button').forEach((b) => b.addEventListener('click', () => showTab(b.dataset.tab)));
function showTab(name) {
  document.querySelectorAll('.tabs button').forEach((b) => b.setAttribute('aria-selected', String(b.dataset.tab === name)));
  document.querySelectorAll('.panel').forEach((p) => (p.hidden = p.id !== name));
  try { localStorage.setItem('cadence.tab', name); } catch {}
}

// ---------- habits ----------
// habit = { id, name, log: { 'YYYY-MM-DD': true } }
function streak(h) {
  let d = new Date();
  if (!h.log[fmt(d)]) d = addDays(d, -1); // today not done yet doesn't break the streak
  let n = 0;
  while (h.log[fmt(d)]) { n++; d = addDays(d, -1); }
  return n;
}

function renderHabits() {
  const list = $('#habit-list');
  list.replaceChildren();
  const t = todayStr();
  $('#today-label').textContent = new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });
  const done = state.habits.filter((h) => h.log[t]).length;
  $('#habit-progress').textContent = `${done}/${state.habits.length} done`;
  $('#habit-empty').hidden = state.habits.length > 0;

  for (const h of state.habits) {
    const check = el('button', { className: 'check' + (h.log[t] ? ' on' : ''), textContent: h.log[t] ? '✓' : '', title: 'Toggle today' });
    check.setAttribute('aria-pressed', String(!!h.log[t]));
    check.setAttribute('aria-label', `Mark ${h.name} for today`);
    check.onclick = () => { h.log[t] ? delete h.log[t] : (h.log[t] = true); save(); renderHabits(); };

    const s = streak(h);
    const info = el('div', {}, el('div', { className: 'name', textContent: h.name }),
      el('div', { className: 'streak', textContent: s ? `🔥 ${s}-day streak` : 'No streak yet' }));

    const week = el('div', { className: 'week' });
    for (let i = 6; i >= 0; i--) {
      const d = addDays(new Date(), -i);
      const cell = el('div', { className: 'day' + (h.log[fmt(d)] ? ' on' : ''), title: fmt(d) }, d.toLocaleDateString(undefined, { weekday: 'narrow' }), el('i'));
      cell.style.cursor = 'pointer';
      cell.onclick = () => { const k = fmt(d); h.log[k] ? delete h.log[k] : (h.log[k] = true); save(); renderHabits(); };
      week.append(cell);
    }
    const del = el('button', { className: 'danger del', textContent: 'Delete' });
    del.onclick = () => { if (confirm(`Delete habit "${h.name}" and its history?`)) { state.habits = state.habits.filter((x) => x.id !== h.id); save(); renderHabits(); } };

    list.append(el('div', { className: 'habit' }, check, info, del, week));
  }
}
$('#habit-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const name = $('#habit-name').value.trim();
  if (!name) return;
  state.habits.push({ id: uid(), name, log: {} });
  $('#habit-name').value = '';
  save(); renderHabits();
});

// ---------- todos ----------
// todo = { id, text, done, due, pri (0 low,1 normal,2 high), created }
let todoFilter = 'all';
document.querySelectorAll('#todo-filters button').forEach((b) => b.addEventListener('click', () => {
  todoFilter = b.dataset.f;
  document.querySelectorAll('#todo-filters button').forEach((x) => x.classList.toggle('on', x === b));
  renderTodos();
}));

function renderTodos() {
  const list = $('#todo-list');
  list.replaceChildren();
  const t = todayStr();
  const items = state.todos
    .filter((x) => todoFilter === 'all' || (todoFilter === 'done') === x.done)
    .sort((a, b) => a.done - b.done || (a.due || '9999').localeCompare(b.due || '9999') || b.pri - a.pri || a.created - b.created);
  $('#todo-empty').hidden = items.length > 0;
  $('#clear-done').hidden = !state.todos.some((x) => x.done);

  for (const x of items) {
    const cb = el('input', { type: 'checkbox', checked: x.done });
    cb.setAttribute('aria-label', `Complete ${x.text}`);
    cb.onchange = () => { x.done = cb.checked; save(); renderTodos(); };
    const li = el('li', { className: 'todo' + (x.done ? ' done' : '') }, cb, el('span', { className: 't', textContent: x.text }));
    if (x.pri === 2) li.append(el('span', { className: 'tag high', textContent: 'High' }));
    if (x.due) li.append(el('span', { className: 'tag' + (!x.done && x.due < t ? ' late' : ''), textContent: x.due === t ? 'Today' : x.due }));
    const del = el('button', { className: 'danger', textContent: '✕', title: 'Delete' });
    del.onclick = () => { state.todos = state.todos.filter((y) => y.id !== x.id); save(); renderTodos(); };
    li.append(del);
    list.append(li);
  }
}
$('#todo-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const text = $('#todo-text').value.trim();
  if (!text) return;
  state.todos.push({ id: uid(), text, done: false, due: $('#todo-due').value || '', pri: +$('#todo-pri').value, created: Date.now() });
  $('#todo-text').value = ''; $('#todo-due').value = ''; $('#todo-pri').value = '1';
  save(); renderTodos();
});
$('#clear-done').addEventListener('click', () => { state.todos = state.todos.filter((x) => !x.done); save(); renderTodos(); });

// ---------- notes ----------
// note = { id, title, body, updated }
let selected = null;
let saveTimer;

function renderNotes() {
  const q = $('#note-search').value.trim().toLowerCase();
  const list = $('#note-list');
  list.replaceChildren();
  const items = state.notes
    .filter((n) => !q || (n.title + ' ' + n.body).toLowerCase().includes(q))
    .sort((a, b) => b.updated - a.updated);
  $('#note-empty').hidden = items.length > 0;
  for (const n of items) {
    const li = el('li', { className: 'note-item' + (n.id === selected ? ' sel' : '') },
      el('b', { textContent: n.title || 'Untitled' }),
      el('span', { textContent: new Date(n.updated).toLocaleDateString() + ' · ' + (n.body.slice(0, 40).replace(/\s+/g, ' ') || 'Empty') }));
    li.onclick = () => { selected = n.id; renderNotes(); renderEditor(); };
    list.append(li);
  }
}
function renderEditor() {
  const n = state.notes.find((x) => x.id === selected);
  $('#editor').hidden = !n;
  $('#editor-empty').hidden = !!n;
  if (!n) return;
  $('#note-title').value = n.title;
  $('#note-body').value = n.body;
  $('#note-meta').textContent = 'Saved ' + new Date(n.updated).toLocaleString();
}
function onEdit() {
  const n = state.notes.find((x) => x.id === selected);
  if (!n) return;
  n.title = $('#note-title').value;
  n.body = $('#note-body').value;
  n.updated = Date.now();
  $('#note-meta').textContent = 'Saving…';
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => { save(); $('#note-meta').textContent = 'Saved ' + new Date(n.updated).toLocaleString(); renderNotes(); }, 400);
}
$('#note-title').addEventListener('input', onEdit);
$('#note-body').addEventListener('input', onEdit);
$('#note-search').addEventListener('input', renderNotes);
$('#note-new').addEventListener('click', () => {
  const n = { id: uid(), title: '', body: '', updated: Date.now() };
  state.notes.push(n); selected = n.id; save(); $('#note-search').value = '';
  renderNotes(); renderEditor(); $('#note-title').focus();
});
$('#note-delete').addEventListener('click', () => {
  if (!confirm('Delete this note?')) return;
  state.notes = state.notes.filter((x) => x.id !== selected);
  selected = null; save(); renderNotes(); renderEditor();
});
window.addEventListener('beforeunload', () => { clearTimeout(saveTimer); save(); });

// ---------- backup ----------
$('#export').addEventListener('click', () => {
  const a = el('a', { href: URL.createObjectURL(new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' })), download: `cadence-${todayStr()}.json` });
  a.click(); URL.revokeObjectURL(a.href);
});
$('#import').addEventListener('change', async (e) => {
  const f = e.target.files[0];
  e.target.value = '';
  if (!f) return;
  try {
    const d = JSON.parse(await f.text());
    if (!Array.isArray(d.habits) || !Array.isArray(d.todos) || !Array.isArray(d.notes)) throw new Error('bad shape');
    if (!confirm('Replace all current data with this backup?')) return;
    state = { habits: d.habits, todos: d.todos, notes: d.notes };
    selected = null; save(); renderAll();
  } catch { alert('That file is not a valid Cadence backup.'); }
});

// ---------- init ----------
function renderAll() { renderHabits(); renderTodos(); renderNotes(); renderEditor(); }
renderAll();
let tab = 'habits';
try { tab = localStorage.getItem('cadence.tab') || tab; } catch {}
showTab(['habits', 'todos', 'notes'].includes(tab) ? tab : 'habits');
// roll the "today" view over at midnight if the tab stays open
setInterval(() => { if (todayStr() !== renderHabits.last) { renderHabits.last = todayStr(); renderHabits(); } }, 60000);
})();
