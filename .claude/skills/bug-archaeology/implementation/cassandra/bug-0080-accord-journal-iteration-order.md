# Bug 0080: AccordJournal Reads Command Diffs in Wrong Order

**Commit**: 5bfd944bcb · **Ticket**: CASSANDRA-20206
**Subsystem**: service/accord/AccordJournal
**Category**: Logic error in condition

---

## Root Cause

`AccordJournal.readCommand` called `journalTable.readAll(key, builder::deserializeNext, true)` where the boolean argument selects the iteration direction — `true` meaning "reverse" (newest-first) when the method requires oldest-first to build up the correct cumulative state. Deserializing diffs in reverse order produces a corrupted command state because each diff is applied on top of the previous one.

### Before / After

```java
// BEFORE
journalTable.readAll(key, builder::deserializeNext, true);
```

```java
// AFTER
journalTable.readAll(key, builder::deserializeNext, false);
```

---

## How to Spot

Whenever a boolean flag is passed to a general-purpose "read all" helper that controls order, review every call site to confirm the chosen direction matches the consumer's assumption about accumulation order. Diff-based or event-sourced structures are especially sensitive: applying the newest entry first and the oldest last yields results that are the inverse of correct.

---

## Test

No test added.
