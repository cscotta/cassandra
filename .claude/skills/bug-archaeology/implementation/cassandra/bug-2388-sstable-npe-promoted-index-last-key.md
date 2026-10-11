# Bug 2388: NPE in SSTable index summarization when promoted index entries are present

**Commit**: 417b175fa4 · **Ticket**: CASSANDRA-4023

## Detection Rule

When code computes the "last key" of a file by comparing byte offsets, it silently breaks for file formats that include variable-length data after the key (e.g. promoted indexes), because the offset check assumes a fixed layout.

## Code Smell

A boolean `lastKey = indexPosition + SHORT_SIZE + len + LONG_SIZE == indexSize` is used to decide whether to deserialize a key, but this arithmetic is only valid for the old, unpromoted-index format; entries with promoted indexes have extra bytes that invalidate the comparison, so `right` (the last key) is never set and remains null.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: format-aware-offset-check
- severity: crash
- subsystem: storage-engine

## Root Cause

`SSTableReader` computed whether a given index entry was the last key in the file by checking that `indexPosition + SHORT_SIZE + len + LONG_SIZE == indexSize`. This assumed entries contain only key + position, which is false for SSTable formats with promoted indexes (which append a variable-length blob). As a result the last key was never identified, `right` stayed null, and any code that used the max key of the SSTable would NPE. The fix always deserializes the key when promoted indexes are present and unconditionally updates `right` on each deserialize.

## Example

```java
// BEFORE
boolean lastKey = indexPosition + SHORT_SIZE + len + LONG_SIZE == indexSize; // wrong for promoted-index format
if (shouldAddEntry || cacheLoading || recreatebloom || firstKey || lastKey)
    decoratedKey = decodeKey(...);
if (lastKey)
    right = decoratedKey;   // never triggers when promoted indexes present → right stays null
RowIndexEntry indexEntry = RowIndexEntry.serializer.deserialize(input, descriptor); // always deserializes
```

```java
// AFTER
boolean lastKeyForUnpromoted = indexPosition + SHORT_SIZE + len + LONG_SIZE == indexSize;
if (shouldAddEntry || cacheLoading || recreatebloom || firstKey || lastKeyForUnpromoted || descriptor.hasPromotedIndexes)
    decoratedKey = decodeKey(...);
right = decoratedKey; // updated whenever key is decoded
// deserialize lazily; use deserializePositionOnly when full entry not needed
if (decoratedKey != null && cacheLoading && keysToLoadInCache.contains(decoratedKey))
    indexEntry = RowIndexEntry.serializer.deserialize(input, descriptor);
if (indexEntry == null)
    indexEntry = RowIndexEntry.serializer.deserializePositionOnly(input, descriptor);
```
