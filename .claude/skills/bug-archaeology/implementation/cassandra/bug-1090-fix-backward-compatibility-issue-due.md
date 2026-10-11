# Bug 1090: Fix Backward Compatibility Issue Due to AbstractBounds Serialization Bug

**Commit**: 24682d21d2 · **Ticket**: CASSANDRA-9857
**Subsystem**: db/ReadResponse.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Pre-3.0 nodes had a bug in `AbstractBounds` serialization: any bound type that was not a plain `Range` was transmitted on the wire as if it were a `Range`. As a result, when a 3.0 coordinator issued a `PartitionRangeReadCommand` with an `ExcludingBounds` or `IncludingExcludingBounds` key range, legacy replicas did not honour the exclusive endpoints and could return rows for keys that should have been excluded. Because 3.0's `LegacyRemoteDataResponse` blindly trusted the set of partitions returned, those extra rows leaked into query results. The fix adds a post-receive filter in `makeIterator` that re-applies the key-range exclusion logic for any legacy response, discarding the first or last partition when the command's key range is exclusive at those boundaries.

### Before / After

```java
// BEFORE
public UnfilteredPartitionIterator makeIterator(CFMetaData metadata, final ReadCommand command)
{
    return new AbstractUnfilteredPartitionIterator() { ... iterates partitions as-is ... };
}
```

```java
// AFTER
public UnfilteredPartitionIterator makeIterator(CFMetaData metadata, final ReadCommand command)
{
    boolean skipFirst = false, skipLast = false;
    if (!partitions.isEmpty() && command instanceof PartitionRangeReadCommand)
    {
        AbstractBounds<PartitionPosition> keyRange = ((PartitionRangeReadCommand)command).dataRange().keyRange();
        boolean isExcludingBounds = keyRange instanceof ExcludingBounds;
        skipFirst = isExcludingBounds && !keyRange.contains(partitions.get(0).partitionKey());
        skipLast  = (isExcludingBounds || keyRange instanceof IncludingExcludingBounds)
                    && !keyRange.contains(partitions.get(partitions.size() - 1).partitionKey());
    }
    final List<ImmutableBTreePartition> toReturn = /* trim skipFirst / skipLast */;
    return new AbstractUnfilteredPartitionIterator() { ... iterates toReturn ... };
}
```

---

## How to Spot

Mixed-version clusters are a prime source of correctness bugs whenever the coordinator applies semantics that older nodes do not support. Any time a new bound or filter type is introduced, review what older serializers produce for that type and whether the coordinator needs to compensate for under-filtering in responses from legacy nodes.

---

## Test

`ReadResponseTest.testLegacyResponseSkipWrongBounds` added, verifying that exclusive-start and exclusive-end boundaries are correctly honoured when the response comes from a legacy node.
