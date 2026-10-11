# Bug 0903: SSTableReader References Held for Duration of Incremental Repair

**Commit**: 675591d1c5 · **Ticket**: CASSANDRA-11739
**Subsystem**: Repair / Compaction
**Category**: Resource leak

---

## Root Cause
`ParentRepairSession` stored the actual `SSTableReader` objects in `sstableMap` (a `Map<UUID, Set<SSTableReader>>`). These reader references were held for the entire lifetime of the repair session, which could be many minutes. If a compaction ran and replaced one of those SSTables with a new file during that window, the old `SSTableReader` kept its file handle open, preventing the OS from reclaiming the disk space. The fix changes `sstableMap` to store only filenames (`Map<UUID, Set<String>>`). A new `getActiveSSTables(UUID)` helper resolves filenames back to live `SSTableReader` objects by consulting the current DataTracker view on demand, filtering out any readers that are no longer in the live set. References (via `Refs`) are then taken only at the point of actual use in `getActiveRepairedSSTableRefs()`.

### Before / After
```java
// BEFORE — stores live SSTableReader references for entire session
public final Map<UUID, Set<SSTableReader>> sstableMap = new HashMap<>();

public synchronized Refs<SSTableReader> getAndReferenceSSTables(UUID cfId) {
    Set<SSTableReader> sstables = sstableMap.get(cfId);
    // file-existence check on each, remove missing ...
    references.put(sstable, ref);
}

// AFTER — stores only filenames; resolves to live readers on demand
public final Map<UUID, Set<String>> sstableMap = new HashMap<>();

private Set<SSTableReader> getActiveSSTables(UUID cfId) {
    // look up current CFS live readers, keep only those whose filename is in sstableMap
}
public synchronized Refs<SSTableReader> getActiveRepairedSSTableRefs(UUID cfId) {
    for (SSTableReader sstable : getActiveSSTables(cfId)) {
        Ref<SSTableReader> ref = sstable.tryRef();
        ...
    }
}
```

---

## How to Spot
Long-lived session objects that cache `SSTableReader` instances by identity are a resource leak risk. The DataTracker/LifecycleTransaction model is designed so that SSTable references should be held only for the duration of a specific operation and then released. Any collection that accumulates `SSTableReader` objects across multiple asynchronous phases (prepare, validate, anti-compact) should instead store stable identifiers (filenames, descriptors) and resolve them to live readers at the point of use.

---

## Test
`ActiveRepairServiceTest` added: verifies that `getActiveRepairedSSTableRefs()` returns only the SSTableReaders still present in the DataTracker after one is removed.
