# Bug 1734: IllegalArgumentException When Reading 1.2 SuperColumn SSTables

**Commit**: 0ff7f9969a · **Ticket**: CASSANDRA-6733
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/IndexedSliceReader.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

SSTables written by Cassandra 1.2 store SuperColumn (SC) index entries using the raw SC name as the comparator key, not the composite key format introduced later. When `IndexedSliceReader` and `SSTableNamesIterator` compared a request's column name against index block boundaries, they used the current composite comparator unconditionally. For old-format SC SSTables this comparison was between incompatible types: the composite name from the query versus the plain SC name in the index, producing an `IllegalArgumentException` from the comparator. The fix detects old-format SC SSTables (`sstable.descriptor.version.hasSuperColumns`) and, for those files only, extracts just the SC portion of the composite name and uses the plain SC comparator when consulting the index.

### Before / After

```java
// BEFORE: always uses the composite comparator against the raw index entry
int index = IndexHelper.indexFor(name, indexList, comparator, false, lastIndexIdx);
// ...
if (comparator.compare(name, indexInfo.firstName) < 0 ...)
```

```java
// AFTER: uses SC-aware helpers that downgrade to plain SC comparator for old SSTables
int index = IndexedSliceReader.indexFor(sstable, name, indexList, comparator, false, lastIndexIdx);
// ...
if (IndexedSliceReader.comparatorForIndex(sstable, comparator)
        .compare(IndexedSliceReader.forIndexComparison(sstable, name), indexInfo.firstName) < 0 ...)
```

---

## How to Spot

When reading on-disk data structures that may have been written by an older version, always check whether the serialized format changed between versions and gate comparator or parser selection on the SSTable descriptor version. Comparator mismatches between index entries and query names are a common source of `IllegalArgumentException` during upgrades.

---

## Test

No test added.
