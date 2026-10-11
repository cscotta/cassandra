# Bug 1992: Key Cache Load Crashes When SSTable Has Been Removed

**Commit**: 43a98c2120 · **Ticket**: CASSANDRA-5706
**Subsystem**: src/java/org/apache/cassandra/service/CacheService.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When loading a saved key cache entry, the code read the boolean `promotedIndexes` flag only _after_ checking whether the SSTable still existed. If the SSTable had been removed, `skipPromotedIndex` was called to advance the stream — but that method did not account for the case where the stream had not yet consumed the boolean flag, causing the stream to be misaligned for all subsequent entries. The fix reads the boolean first, then performs the existence check and branches correctly.

### Before / After

```java
// BEFORE
reader = findDesc(generation, cfs.getSSTables());
if (reader == null)
{
    RowIndexEntry.serializer.skipPromotedIndex(input);  // consumed wrong bytes
    return null;
}
RowIndexEntry entry;
if (input.readBoolean())   // boolean read after null check
    entry = ...deserialize...
```

```java
// AFTER
reader = findDesc(generation, cfs.getSSTables());
boolean promotedIndexes = input.readBoolean();  // always read first
if (reader == null)
{
    if (promotedIndexes)
        RowIndexEntry.serializer.skip(input, Descriptor.Version.CURRENT);
    return null;
}
RowIndexEntry entry = promotedIndexes
    ? RowIndexEntry.serializer.deserialize(input, reader.descriptor.version)
    : reader.getPosition(...);
```

---

## How to Spot

When a serialized format includes a flag that governs what follows in the stream, that flag must be consumed before any conditional early-return. Any `readX()` call that appears _before_ the flag but logically depends on it will leave the stream in an inconsistent state for all subsequent reads.

---

## Test

No test added.
