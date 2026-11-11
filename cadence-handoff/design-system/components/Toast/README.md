A short confirmation with Undo, shown after completing or deleting something.

**You provide:** a past-tense message of two to four words and the undo action.

- Appears above the tab bar for 5 seconds; a 2px `accent` line drains to show the time left. One toast at a time: a new one replaces the old.
- Undo restores the item, its position and its beat. It makes confirm dialogs unnecessary for completions.
- Never used for errors, tips or promotion.
- **Motion:** rises 16px and fades in over `duration-base` (`ease-out`); leaves with a fade over `duration-quick`. **Haptic:** none on appear (the completion already played one); `haptic-tick` on Undo.
- Screen readers announce it politely and it stays reachable for 10 seconds.
