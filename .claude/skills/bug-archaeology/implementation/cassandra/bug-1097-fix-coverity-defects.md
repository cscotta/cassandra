# Bug 1097: Fix Coverity Defects — Dead Call in SerializationHeader

**Commit**: 00f0feb31b · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/SerializationHeader.java
**Category**: Logic error in condition

---

## Root Cause

`SerializationHeader` called `stats.updateColumnSetPerRow(sstable.getTotalColumnsSet(), sstable.getTotalRows())` during header construction. The underlying `EncodingStats.Collector.updateColumnSetPerRow(long, long)` accumulated a running sum of columns and rows to compute an average, but that accumulated average was never actually used when producing the final `EncodingStats` — the fields `totalColumnsSet` and `rows` were dead state that had no effect on the serialized header. The call was therefore a no-op with side-effect-free dead code (flagged by Coverity), and its companion two-argument overload and all related fields were removed entirely in the companion commit 1098.

### Before / After

```java
// BEFORE
stats.updateTimestamp(sstable.getMinTimestamp());
stats.updateLocalDeletionTime(sstable.getMinLocalDeletionTime());
stats.updateTTL(sstable.getMinTTL());
stats.updateColumnSetPerRow(sstable.getTotalColumnsSet(), sstable.getTotalRows());
```

```java
// AFTER
stats.updateTimestamp(sstable.getMinTimestamp());
stats.updateLocalDeletionTime(sstable.getMinLocalDeletionTime());
stats.updateTTL(sstable.getMinTTL());
// call removed — accumulated value was never read
```

---

## How to Spot

When a collector method accumulates state but no code ever reads the accumulated result to influence output, the accumulation is dead code. Coverity's "result not used" and "dead store" detectors reliably surface these; manual review should check whether every field written in a `Collector.update*` method is actually consumed in the `Collector.get()` or equivalent finalizing method.

---

## Test

No test added.
