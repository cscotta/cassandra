# Bug 0278: Upgrader Sets Target Directory After First Record Is Written

**Commit**: 17810295ca · **Ticket**: CASSANDRA-17849
**Subsystem**: db/compaction/Upgrader
**Category**: Logic error in condition

---

## Root Cause

A misplaced statement in the `Upgrader.upgrade()` loop caused `iter.setTargetDirectory()` to be
called at the start of the *next* iteration rather than before the first `writer.append()` call.
The Java `while` loop body was written as:

```
while (iter.hasNext())
    iter.setTargetDirectory(...);   // ← only this line is in the loop
    writer.append(iter.next());     // ← always executed once, outside the loop
```

Without braces the second statement was outside the loop, so `setTargetDirectory` was called
once per iteration but `append` was called only once total after the loop completed.  For a
single-SSTable upgrade the table was written with no target directory set for the first (and
only) record, producing incorrect metadata and making the test non-deterministic.

### Before / After

```java
// BEFORE – missing braces; setTargetDirectory inside loop, append outside
while (iter.hasNext())
    iter.setTargetDirectory(writer.currentWriter().getFilename());
    writer.append(iter.next());
```

```java
// AFTER – setTargetDirectory called once before the loop; append inside loop
iter.setTargetDirectory(writer.currentWriter().getFilename());
while (iter.hasNext())
    writer.append(iter.next());
```

---

## How to Spot

Single-statement loop bodies without braces are a classic source of logic errors when lines are
added or reordered during refactoring.  In code review, any `while`/`for` without braces where
the indented line below the loop header is not the intended sole body statement should be
flagged.  Here, the intent was clearly to set the target directory once (not per-record) and
then iterate; moving `setTargetDirectory` before the loop and adding braces—or enforcing a
brace-always style—would have prevented the bug.

---

## Test

No new test added; the fix corrected a regression caught by the existing
`StandaloneUpgraderOnSStablesTest`, which became non-flaky after the fix.
