# Bug 0532: Table metadata's last-updated timestamp incorrectly taken from snapshot timestamp

**Commit**: 615b5a097cf27a65fec2a9f613150ab83ea8bc7a · **Ticket**: #14504

## Detection Rule

When a metadata builder sets a timestamp field on `this` using a value from a child object (e.g., `snapshot.timestampMillis()`), verify that the intent is really to propagate that child's time rather than to record the wall-clock time of the current operation.

## Code Smell

`this.lastUpdatedMillis = snapshot.timestampMillis()` inside a builder method whose purpose is to record when the metadata *file* was written, replacing an earlier `System.currentTimeMillis()` assignment.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: core/table-metadata

## Root Cause

`TableMetadata.Builder` updated `lastUpdatedMillis` with the snapshot's own timestamp whenever a snapshot was added or a ref was set to a newly-added snapshot. Because a snapshot's timestamp is set at snapshot-creation time (potentially on a different host or earlier in the same transaction), the metadata file's `last-updated-ms` field ended up reflecting when data was written rather than when the metadata was committed. On rollback the effect was more severe: rolling back to an old snapshot copied that old snapshot's timestamp as the current metadata timestamp, making the rollback appear to have happened in the past.

## Example

```java
// BEFORE
void addSnapshot(Snapshot snapshot) {
    this.lastUpdatedMillis = snapshot.timestampMillis(); // wrong source
    this.lastSequenceNumber = snapshot.sequenceNumber();
    snapshots.add(snapshot);
}

void setRef(String name, SnapshotRef ref) {
    if (isAddedSnapshot(snapshotId)) {
        this.lastUpdatedMillis = snapshot.timestampMillis(); // propagates old ts
    }
    snapshotLog.add(new SnapshotLogEntry(lastUpdatedMillis, ref.snapshotId()));
}
```

```java
// AFTER
void addSnapshot(Snapshot snapshot) {
    // lastUpdatedMillis is NOT updated here; caller controls it
    this.lastSequenceNumber = snapshot.sequenceNumber();
    snapshots.add(snapshot);
}

void setRef(String name, SnapshotRef ref) {
    // for rollback, use current wall-clock time already stored in lastUpdatedMillis
    long timeOfChange =
        isAddedSnapshot(snapshotId) ? snapshot.timestampMillis() : this.lastUpdatedMillis;
    snapshotLog.add(new SnapshotLogEntry(timeOfChange, ref.snapshotId()));
}
```
