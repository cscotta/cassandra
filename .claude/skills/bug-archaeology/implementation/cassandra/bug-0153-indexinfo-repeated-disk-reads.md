# Bug 0153: Repeated Disk Reads of Same IndexInfo Block During Partition Iteration

**Commit**: 049160e70a · **Ticket**: CASSANDRA-19557
**Subsystem**: `io/sstable/format/big/IndexState`
**Category**: Wrong constant / default value

---

## Root Cause

When iterating rows within a large partition that uses `ShallowIndexedEntry`, `IndexState.index(i)` was called for every row without any caching. For `ShallowIndexedEntry` the `IndexInfoRetriever` reads the `IndexInfo` block from disk on each call, meaning every row processed within the same index block caused a redundant disk read. A simple last-value cache keyed on the integer index eliminates redundant I/O entirely since sequential row iteration re-requests the same index block many times in a row.

### Before / After

```java
// BEFORE
public IndexInfo index(int i) throws IOException
{
    return indexInfoRetriever.columnsIndex(i);
}
```

```java
// AFTER
private int cachedIndexIdx = Integer.MIN_VALUE;
private IndexInfo cachedIndexInfo;

public IndexInfo index(int i) throws IOException
{
    if (i == cachedIndexIdx)
        return cachedIndexInfo;
    cachedIndexInfo = indexInfoRetriever.columnsIndex(i);
    cachedIndexIdx = i;
    return cachedIndexInfo;
}
```

---

## How to Spot

Any hot-path method that delegates unconditionally to a retriever/loader without memoizing the result is suspect when: (1) the retriever's backing storage is not in-memory, and (2) the call site is inside an inner loop where the argument changes infrequently relative to loop iterations. Review callers of `columnsIndex` (or similar read-through accessors) for whether consecutive calls with identical arguments are expected.

---

## Test

No test added; the fix is a pure performance/correctness improvement that is difficult to unit-test without mocking disk I/O.
