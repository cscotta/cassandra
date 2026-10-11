# Bug 1461: Integer Overflow When Sorting Commit Log Segments for Replay

**Commit**: 96c045f857 · **Ticket**: CASSANDRA-7992
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogSegment.java
**Category**: Wrong constant / default value

---

## Root Cause

The `Comparator` used to sort commit log segment files for replay computed the ordering by casting the difference of two `long` segment IDs to `int`. When the two IDs differed by more than `Integer.MAX_VALUE` the subtraction overflowed, producing a result with the wrong sign and therefore a wrong sort order. Segment IDs are derived from system timestamps in milliseconds, so on long-lived clusters the gap between the oldest and newest segment on disk easily exceeds 2^31. The fix replaces the subtraction with `Long.compare`, which has no overflow risk.

### Before / After

```java
// BEFORE
return (int) (desc.id - desc2.id);
```

```java
// AFTER
return Long.compare(desc.id, desc2.id);
```

---

## How to Spot

Any `Comparator` that implements ordering via integer subtraction of two numeric values is susceptible to overflow if the values can differ by more than the range of the subtraction type. The safe idiom is always to use `Integer.compare` or `Long.compare` (or their static equivalents) instead of subtraction. Code review should flag any `(int)(a - b)` or `(int)(a.id - b.id)` pattern in a comparator body.

---

## Test

No test added.
