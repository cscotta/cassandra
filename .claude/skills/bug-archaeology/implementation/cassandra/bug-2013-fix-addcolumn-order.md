# Bug 2013: Wrong Column Insertion Order in BatchlogManager Causes Schema Mismatch

**Commit**: 22d1173021 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/BatchlogManager.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`BatchlogManager.getBatchlogMutationFor()` builds the batchlog row by adding columns in the order `""` (sentinel), `"written_at"`, `"data"`. However, `ArrayBackedSortedColumns` stores columns in sorted order by name, and `"data"` sorts before `"written_at"` lexicographically. Adding `"written_at"` before `"data"` violated the sorted-order invariant of the backing store, potentially causing the column for `"data"` to be misplaced or the final mutation to have incorrect column ordering when serialized or replayed. The fix inserts `"data"` before `"written_at"` to match the lexicographic sort order.

### Before / After

```java
// BEFORE
cf.addColumn(new Column(columnName(""), ByteBufferUtil.EMPTY_BYTE_BUFFER, timestamp));
cf.addColumn(new Column(columnName("written_at"), writtenAt, timestamp));
cf.addColumn(new Column(columnName("data"), data, timestamp));
```

```java
// AFTER
cf.addColumn(new Column(columnName(""), ByteBufferUtil.EMPTY_BYTE_BUFFER, timestamp));
cf.addColumn(new Column(columnName("data"), data, timestamp));
cf.addColumn(new Column(columnName("written_at"), writtenAt, timestamp));
```

---

## How to Spot

When manually adding columns to a sorted-columns data structure, the insertion order must match the sort order of the column names. Review all `addColumn()` call sequences in `ArrayBackedSortedColumns` users to confirm they insert in lexicographic (or comparator) order. Out-of-order inserts may silently corrupt the column family or cause assertions to fire during replay.

---

## Test

No test added.
