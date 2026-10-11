# Bug 0945: SSTable Reference Leak During Parallel Repair

**Commit**: 477014b8cd · **Ticket**: CASSANDRA-11215
**Subsystem**: Compaction / Repair
**Category**: Resource leak

---

## Root Cause
In `CompactionManager.doValidationCompaction`, the code obtained a `RefViewFragment` (a reference-counted snapshot of the SSTable set), iterated over it to build the `sstablesToValidate` set, and then released the fragment. Between the release of `sstableCandidates` and the subsequent call to `Refs.tryRef(sstablesToValidate)` there was no synchronisation, so a concurrent thread could close one of the candidate SSTables between the two steps, making it impossible to take a reference — and also causing the fragment release to be skipped entirely on the error path because the early-return was missing.

The fix extracts the entire selection and reference-taking into a new `synchronized` method `getSSTablesToValidate`, so the `RefViewFragment` is held for the full duration of `tryRef`. The try-with-resources block on the fragment ensures it is always released, and a `null` return from the helper is used as a clean signal that the parent repair session was already removed.

### Before / After
```java
// BEFORE (non-synchronized, fragment released before tryRef)
ColumnFamilyStore.RefViewFragment sstableCandidates = cfs.selectAndReference(...);
// ... build sstablesToValidate ...
sstables = Refs.tryRef(sstablesToValidate);
if (sstables == null) throw new RuntimeException(...);
sstableCandidates.release();        // skipped on throw above
prs.addSSTables(...);

// AFTER (synchronized, fragment held until tryRef completes)
private synchronized Refs<SSTableReader> getSSTablesToValidate(...) {
    try (ColumnFamilyStore.RefViewFragment sstableCandidates = cfs.selectAndReference(...)) {
        // ... build sstablesToValidate ...
        sstables = Refs.tryRef(sstablesToValidate);
        // fragment closed by try-with-resources
    }
    prs.addSSTables(...);
    return sstables;
}
```

---

## How to Spot
Any time a reference-counted object is released before a second reference is acquired from the same object, there is a window where the underlying resource may be destroyed. Look for patterns where `release()` / `close()` on a view or snapshot is called before `tryRef` / `ref` on members of that snapshot — especially in non-synchronized blocks where concurrent compaction or streaming can reclaim the file.

---

## Test
No test added.
