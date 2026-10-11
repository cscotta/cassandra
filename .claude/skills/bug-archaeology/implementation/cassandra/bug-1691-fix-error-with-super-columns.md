# Bug 1691: Fix Error with Super Columns in Mixed 1.2-2.0 Cluster

**Commit**: e1a90936c7 · **Ticket**: CASSANDRA-6966
**Subsystem**: src/java/org/apache/cassandra/db/SuperColumns.java
**Category**: Missing null / bounds check

---

## Root Cause

When translating a modern CQL slice filter back to the old super-column format for compatibility with 1.2 nodes, `SuperColumns.sliceFilterToSC()` called `filter.withUpdatedSlice(subName(start), subName(finish))` without guarding against a `null` return from `subName()`. When the composite column name had no sub-column component, `subName()` returns `null`, which was passed directly to the slice constructor and caused a NullPointerException on the 1.2 node. The fix wraps both values: if `subName` returns `null`, substitute `ByteBufferUtil.EMPTY_BYTE_BUFFER` as the open-ended boundary. The same commit also corrects the branch condition from `compositesToGroup == 0` to `compositesToGroup == -1`, which is the sentinel value meaning "sub-column slice".

### Before / After

```java
// BEFORE – null subName passed to slice, wrong sentinel value
else if (filter.compositesToGroup == 0 && type.types.get(0).compare(scName(start), scName(finish)) == 0)
{
    return new SCFilter(scName(start), filter.withUpdatedSlice(subName(start), subName(finish)));
}
```

```java
// AFTER – null guard + correct sentinel
else if (filter.compositesToGroup == -1 && type.types.get(0).compare(scName(start), scName(finish)) == 0)
{
    ByteBuffer newStart = subName(start);
    ByteBuffer newFinish = subName(finish);
    return new SCFilter(scName(start),
                        filter.withUpdatedSlice(newStart  == null ? ByteBufferUtil.EMPTY_BYTE_BUFFER : newStart,
                                                newFinish == null ? ByteBufferUtil.EMPTY_BYTE_BUFFER : newFinish));
}
```

---

## How to Spot

Any helper that extracts a sub-component from a composite can return null for a partial composite; callers that pass the result directly to constructors expecting non-null byte buffers need explicit null-to-empty guards. Also verify sentinel/magic constant values when branching on filter type fields.

---

## Test

No new test added in this commit.
