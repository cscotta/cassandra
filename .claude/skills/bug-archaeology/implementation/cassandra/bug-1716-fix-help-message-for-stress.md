# Bug 1716: Fix Help Message for Stress counter_write

**Commit**: 92381fd269 · **Ticket**: CASSANDRA-6824
**Subsystem**: tools/stress/settings/Command, StressAction
**Category**: Wrong constant / default value

---

## Root Cause

The `Command` enum used inconsistent names: enum constants like `COUNTERWRITE`, `COUNTERREAD`, `RANGESLICE`, `IRANGESLICE`, and `READMULTI` did not match the underscore-separated names users expected to type on the command line (e.g., `counter_write`). The lookup table only registered the lowercase enum name, so `counter_write` would not resolve to `COUNTERWRITE`. The fix renames the constants to use underscores (`COUNTER_WRITE`, `COUNTER_READ`, etc.) and builds the lookup table by registering both the underscore form and the no-underscore form, allowing both spellings to work.

### Before / After

```java
// BEFORE
COUNTERWRITE(true, "Counter1", "SuperCounter1", "counter_add", ...),
COUNTERREAD(false, "Counter1", "SuperCounter1", "counter_get", ...),
```

```java
// AFTER
COUNTER_WRITE(true, "Counter1", "SuperCounter1", "counter_add", ...),
COUNTER_READ(false, "Counter1", "SuperCounter1", "counter_get", ...),
```

```java
// BEFORE
lookup.put(cmd.toString().toLowerCase(), cmd);
if (cmd.extraName != null)
    lookup.put(cmd.extraName, cmd);
```

```java
// AFTER
for (String name : cmd.names)
    lookup.put(name, cmd);
// names includes both "counter_write" and "counterwrite" spellings
```

---

## How to Spot

When an enum is used as a CLI command dispatcher, verify that every user-visible name maps correctly to its enum constant in the lookup table. Missing underscore variants mean some documented command spellings silently fail to dispatch.

---

## Test

No test added.
