One task: square check, title, due time, duration, tags and a priority flag.

**You provide:** title, due (time today, weekday this week, or date), optional duration, up to two tags, and whether it is flagged.

- Meta order is fixed: due, duration, tags. Missing values are omitted, not shown as dashes.
- **Priority** is one level: flagged or not. The flag is `accent`, filled, at the row's trailing edge with the label "Priority". No red, no three-tier colours.
- An overdue task shows "Since Mon" in `ink` weight 600 instead of a red date. It moves to the top of Today and becomes a Next up candidate.
- Done tasks strike through, turn `ink-muted`, and move to the Done group after the Undo toast expires.
- Duration is what makes Next up honest: with it, the app can prefer a 10-minute task when the next event is close.
