# Bug 2145: Deadlock in Multithreaded Compaction When Rows Are Empty

**Commit**: 134e8c78d2 · **Ticket**: CASSANDRA-4492
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LazilyCompactedRow.java, src/java/org/apache/cassandra/db/compaction/CompactionTask.java
**Category**: Deadlock

---

## Root Cause

`LazilyCompactedRow.close()` was declared `private`, preventing it from being called via the `AbstractCompactedRow` base class. When `CompactionTask` encountered an empty compacted row and called `continue` to skip it, `close()` was never invoked on the `LazilyCompactedRow`, leaving its internal `IColumnIterator` resources (including iterators backed by file I/O) unreleased. Under multithreaded compaction this caused threads to deadlock waiting on locked resources that the abandoned iterator held. The fix made `close()` public by implementing the `Closeable` interface on `AbstractCompactedRow` and adding a no-op `close()` to `PrecompactedRow`.

### Before / After

```java
// BEFORE – private, never called on skip
private void close() { for (IColumnIterator row : rows) { ... } }

// CompactionTask:
if (row.isEmpty())
    continue;  // close() never called
```

```java
// AFTER – public via Closeable
public void close() { for (IColumnIterator row : rows) { ... } }

// CompactionTask:
if (row.isEmpty())
{
    row.close();
    continue;
}
```

---

## How to Spot

When iterating a collection and using `continue` to skip an element, check whether the element holds resources that must be released. A `close()` method that is `private` on a concrete class but needed by an abstract base class is a clear sign that the cleanup path is broken for callers that only hold a reference to the abstract type.

---

## Test

No test added.
