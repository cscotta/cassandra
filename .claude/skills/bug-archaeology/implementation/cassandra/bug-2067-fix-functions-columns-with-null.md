# Bug 2067: writetime() Returns Garbage and ttl() Crashes on Null or Deleted Columns

**Commit**: 65b9093d49 · **Ticket**: CASSANDRA-5341
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/Selection.java
**Category**: Missing null / bounds check

---

## Root Cause

`Selection.ResultSetBuilder.add()` is called for every column in a row, including columns that are null or marked for deletion. Before the fix, the code called `c.timestamp()` and checked `c instanceof ExpiringColumn` unconditionally, so a null or deleted column produced a meaningless timestamp (or an NPE) and an incorrect TTL. The timestamps array was never initialized to a sentinel, so callers of `writetime()` received raw uninitialized or garbage values instead of `null`. The fix introduces a shared `isDead(c)` helper and uses `-1` as a sentinel in the timestamps array, then converts that sentinel to a `null` ByteBuffer in the final `writetime()` computation.

### Before / After

```java
// BEFORE – crashes or returns garbage for null/deleted columns
public void add(IColumn c)
{
    current.add(c == null || c.isMarkedForDelete() ? null : value(c));
    if (timestamps != null)
        timestamps[current.size() - 1] = c.timestamp(); // NPE if c == null
    if (ttls != null)
    {
        int ttl = -1;
        if (c instanceof ExpiringColumn)  // also checks dead columns
            ttl = ...;
        ttls[current.size() - 1] = ttl;
    }
}
```

```java
// AFTER – guards every dereference with isDead()
public void add(IColumn c)
{
    current.add(isDead(c) ? null : value(c));
    if (timestamps != null)
        timestamps[current.size() - 1] = isDead(c) ? -1 : c.timestamp();
    if (ttls != null)
    {
        int ttl = -1;
        if (!isDead(c) && c instanceof ExpiringColumn)
            ttl = ...;
        ttls[current.size() - 1] = ttl;
    }
}

// writetime() now returns null for the sentinel
long ts = rs.timestamps[idx];
return ts >= 0 ? ByteBufferUtil.bytes(ts) : null;
```

---

## How to Spot

Whenever an array or collection is pre-filled in one method and read in another, confirm that every element is written with a proper sentinel before it can be read back. The absence of a "null column" branch in any method that unconditionally dereferences a column reference is a strong signal of this class of bug.

---

## Test

No test added.
