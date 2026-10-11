**Commit**: 167e2f71f0 · **Ticket**: KAFKA-17713
**Subsystem**: metadata (MetadataProvenance, SnapshotGenerator)
**Category**: Logic error in condition

**Root cause**
The metadata snapshot generator could schedule a snapshot emission at an offset that fell in the middle of a multi-record transaction batch. Snapshotting at a non-batch-aligned offset produces a snapshot that cannot be correctly replayed (the partial batch is neither included nor excluded cleanly). The root cause was that `scheduleEmit` did not check whether the current provenance pointed to the last offset of a completed batch before scheduling.

**Before/After**
```java
// Before – snapshot scheduled regardless of batch boundary
void scheduleEmit(String reason, long snapshotOffset) {
    snapshotGenerator.scheduleEmit(snapshotOffset);
}

// After – only schedule when offset is batch-aligned
void maybeScheduleEmit(String reason, long snapshotOffset) {
    if (provenance.isOffsetBatchAligned()) {
        snapshotGenerator.scheduleEmit(snapshotOffset);
    }
}
```

**How to spot**
Any snapshot or checkpoint mechanism that records the "current offset" must verify that the offset is at a transaction/batch boundary before committing; check that the triggering code path provides alignment guarantees rather than relying on caller discipline.

**Test**
Unit tests added in `SnapshotGeneratorTest` covering non-aligned provenance rejection.
