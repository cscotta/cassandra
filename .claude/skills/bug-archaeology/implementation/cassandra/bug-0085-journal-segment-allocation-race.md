# Bug 0085: Journal isFlushed Misses the Currently-Active Segment After a Switch

**Commit**: 1866b8ec73 · **Ticket**: CASSANDRA-19866
**Subsystem**: journal/Journal
**Category**: Race condition / missing synchronization

---

## Root Cause

`Journal.isFlushed(RecordPointer)` checked the immutable `Segments` snapshot (`segments.get()`) for flush status without first consulting the live `currentSegment`. Between the moment a segment is promoted to "current" and the moment it is registered in the `Segments` snapshot, a concurrent caller asking whether a record in that segment is flushed would not find it in the snapshot and could receive an incorrect answer. The fix checks `currentSegment` first and only falls back to the snapshot for older segments.

### Before / After

```java
// BEFORE
public boolean isFlushed(RecordPointer recordPointer)
{
    return segments.get().isFlushed(recordPointer);
}
```

```java
// AFTER
public boolean isFlushed(RecordPointer recordPointer)
{
    Segment<K, V> current = currentSegment;
    if (current.descriptor.timestamp == recordPointer.segment)
        return current.isFlushed(recordPointer.position);
    return segments.get().isFlushed(recordPointer);
}
```

---

## How to Spot

When a mutable "current" pointer and an immutable snapshot collection both track the same set of items, any query that consults only the snapshot has a TOCTOU window where the current item is not yet in the snapshot. Always check the live pointer first, then fall back to the snapshot for historical items.

---

## Test

No test added.
