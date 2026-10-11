# Bug 2131: Validation Compaction Crashes or Leaks Resources on Empty Rows

**Commit**: 1c2a69c358 · **Ticket**: CASSANDRA-5136
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Resource leak

---

## Root Cause

The validation compaction loop previously used `Iterators.filter(iter, Predicates.notNull())` to skip null rows, then passed every remaining row to `validator.add()`. However, rows with no live columns (tombstone-only or fully expired rows) produce an `AbstractCompactedRow` where `isEmpty()` returns `true`. Passing such a row to `validator.add()` without closing it caused the row's underlying resources to leak. The fix removes the notNull filter, instead explicitly checking `row.isEmpty()` and calling `row.close()` before skipping validation for empty rows.

### Before / After

```java
// BEFORE — notNull filter passes empty rows to validator; resources leak
Iterator<AbstractCompactedRow> nni = Iterators.filter(iter, Predicates.notNull());
while (nni.hasNext()) {
    AbstractCompactedRow row = nni.next();
    validator.add(row);
}
```

```java
// AFTER — empty rows are closed and skipped
while (iter.hasNext()) {
    AbstractCompactedRow row = iter.next();
    if (row.isEmpty())
        row.close();
    else
        validator.add(row);
}
```

---

## How to Spot

Whenever an iterator of closeable objects is filtered, confirm that filtered-out items are explicitly closed. A filter that silently skips objects without releasing their resources is a resource leak. Look for patterns where `Iterators.filter` or stream `.filter()` is applied to objects implementing `Closeable`.

---

## Test

No test added in this commit.
