# Bug 1839: Fix Attempting to Flush Empty Rows

**Commit**: 1ca459d146 · **Ticket**: CASSANDRA-6374
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Logic error in condition

---

## Root Cause

During a Memtable flush, `Memtable.flushAndSignal()` iterated all entries and wrote each `ColumnFamily` to the SSTable writer. A `ColumnFamily` can be left with zero columns after the secondary-index column-removal step (`removeDeletedColumnsOnly`). Writing a row with no columns and no deletion marker produces a corrupt / useless SSTable entry and can trigger assertion failures downstream. The fix skips the `writer.append()` call for any `ColumnFamily` that has no columns and is not itself marked for deletion, ensuring only meaningful rows are written.

### Before / After

```java
// BEFORE — always appended, even empty non-deleted rows
writer.append((DecoratedKey) entry.getKey(), cf);
```

```java
// AFTER — skip rows that are empty and not tombstones
if (cf.getColumnCount() > 0 || cf.isMarkedForDelete())
    writer.append((DecoratedKey) entry.getKey(), cf);
```

---

## How to Spot

After any in-place filtering that removes columns from a `ColumnFamily`, check whether the result can be empty. If subsequent code assumes the object is non-empty (e.g. a serialisation writer), add a guard before the write. This pattern applies broadly: mutate-then-validate before I/O.

---

## Test

No test added.
