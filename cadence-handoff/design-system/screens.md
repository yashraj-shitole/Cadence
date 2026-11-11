# Screens

Eight screens and one sheet. Tabs: Today, Habits, Tasks, Notes, with Quick add in the centre. Settings opens from Today's top-right icon.

| Screen | Purpose | Order of content | Primary action |
| --- | --- | --- | --- |
| Today | Know the next action and the day's progress | Date and title → BeatBar → NextUp → Habits due (by time of day) → Tasks due | Mark done (in NextUp) |
| Habits | See every habit and its week | Title → Morning, Afternoon, Evening groups of HabitRow | Check on a row |
| Habit detail | Understand one habit without judgement | Back → title and cue → streak `numeral` and "25 of 27 active days" → ConsistencyGrid → Minimum, Schedule, Rest days rows | Log today |
| Tasks | Decide what to do this week | Title → filter chips → Today, This week, Done groups of TaskRow | Check on a row |
| Notes | Find or start a note | Title → SearchField → tag chips → Pinned → All notes | Open a note |
| Note editor | Write without chrome | Back, pin, more → title → body with checklist lines → tags | Typing; saves itself |
| Quick add | Capture in one tap | Segmented type → title Field → optional chips → button | Add task / Add habit / Save note |
| Settings | Set rhythm, look and feel | Feel → Appearance (colour palette first) → Rhythm → Data | None; every control applies at once |

## Rules that hold on every screen

- One `display` title, top left, on the backdrop. No navigation bar background.
- The list ends with bottom padding of `tabbar-height` + `space-8` so the last row clears the tab bar.
- Empty states are one sentence and one action: "No habits yet. Add one you can do in two minutes." with a glass button.
- Today shows at most the beats due today. Anything else is one tab away.
- Completed rows stay in place until the Undo toast expires, then settle into their done position on the next open.
