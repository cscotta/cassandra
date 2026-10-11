# Bug 1200: StreamSession Uses Non-Canonical SSTable Instances During Streaming

**Commit**: 61aeb91093 · **Ticket**: CASSANDRA-9700
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamSession.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamSession` iterated `view.sstablesInBounds()` to find SSTables to stream, and then checked membership using `filteredSSTables.contains(sstable)`. The bounds view can return "early opened" SSTable instances that are distinct objects from the canonical instances tracked in `ColumnFamilyStore.CANONICAL_SSTABLES`, so the `contains` check could silently exclude valid SSTables or include stale ones. The fix builds a lookup map from canonical instances keyed by identity, then replaces any early-opened instance with its canonical counterpart before adding it to the stream set.

### Before / After

```java
// BEFORE
List<SSTableReader> filteredSSTables = ColumnFamilyStore.CANONICAL_SSTABLES.apply(view);
for (AbstractBounds<PartitionPosition> rowBounds : rowBoundsList)
    for (SSTableReader sstable : view.sstablesInBounds(rowBounds))
        if (filteredSSTables.contains(sstable) && (!isIncremental || !sstable.isRepaired()))
            sstables.add(sstable);
```

```java
// AFTER
Map<SSTableReader, SSTableReader> permittedInstances = new HashMap<>();
for (SSTableReader reader : ColumnFamilyStore.CANONICAL_SSTABLES.apply(view))
    permittedInstances.put(reader, reader);

for (AbstractBounds<PartitionPosition> rowBounds : rowBoundsList)
    for (SSTableReader sstable : view.sstablesInBounds(rowBounds)) {
        if (isIncremental && sstable.isRepaired()) continue;
        sstable = permittedInstances.get(sstable);
        if (sstable != null)
            sstables.add(sstable);
    }
```

---

## How to Spot

When code queries a view for objects matching some spatial or range criterion and then cross-checks the result against a separately obtained "allowed" collection, verify that both collections contain the same object instances. If one source can produce early-opened, shadow, or proxy versions of the same logical object, a `contains` or equality check using object identity (or an `equals` that delegates to identity) will silently drop valid entries. The fix is always to invert the lookup: build a map from the authoritative set and replace query results with canonical handles.

---

## Test

No test added.
