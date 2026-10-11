# Bug 0802: cassandra-stress ALWAYS Truncate Skipped Before Warmup

**Commit**: d582d03408 · **Ticket**: CASSANDRA-12695
**Subsystem**: cassandra-stress / tools
**Category**: Logic error in condition

---

## Root Cause
The stress tool supports three truncation modes: `NEVER`, `ONCE` (truncate once before the run), and `ALWAYS` (truncate before every thread starts). The pre-warmup truncation check only fired for `ONCE`, meaning when `truncate=ALWAYS` was used in threaded mode (`threadCount != -1`), no truncation ever happened. The `ALWAYS` mode was silently ignored even though it is the mode that signals the user explicitly wants the table cleared before every parallel thread group starts.

### Before / After
```java
// BEFORE
if (settings.command.truncate == SettingsCommand.TruncateWhen.ONCE)
    settings.command.truncateTables(settings);

// AFTER
if ((settings.command.truncate == SettingsCommand.TruncateWhen.ONCE) ||
    ((settings.rate.threadCount != -1) && (settings.command.truncate == SettingsCommand.TruncateWhen.ALWAYS)))
    settings.command.truncateTables(settings);
```

---

## How to Spot
When reviewing multi-branch enum dispatch (especially for `TruncateWhen`-style enumerations), verify that every meaningful non-default enum value has an explicit handling path. A guard that only tests for one variant while silently skipping others is a classic silent-skip bug. Pay particular attention when a mode name like `ALWAYS` implies unconditional behavior—any condition that gates it should be immediately suspicious.

---

## Test
No test added.
