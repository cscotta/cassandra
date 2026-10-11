# Bug 1856: CommitLog Replay List Split Uses Wrong Regex Pattern

**Commit**: 3c229c0d90 · **Ticket**: n/a
**Subsystem**: db/commitlog
**Category**: Logic error in condition

---

## Root Cause

`String.split(".")` in Java treats the argument as a regular expression, and `.` matches any character rather than a literal period. The result is that every character in the `keyspace.table` string becomes a split point, producing an array of empty strings instead of the expected two-element `[keyspace, table]` array. The pair-length check then throws `IllegalArgumentException` for every valid entry in `cassandra.replayList`.

### Before / After

```java
// BEFORE
String[] pair = rawPair.trim().split(".");
```

```java
// AFTER
String[] pair = rawPair.trim().split("\\.");
```

---

## How to Spot

Whenever `String.split()` is called with a single-character pattern that has regex meaning (`.`, `|`, `*`, `+`), verify it is properly escaped with `\\.` or wrapped in `Pattern.quote()`. This class of error is invisible at compile time and only surfaces at runtime with all-empty splits.

---

## Test

No test added.
