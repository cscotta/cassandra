# Bug 0419: Snapshot event production always triggers unnecessary metadata refresh after merge append

**Commit**: c6b252e97eace78cf31df1d72ff91e7e641049da · **Ticket**: #14709

## Detection Rule

When a method calls `ops().refresh()` unconditionally to look up data that may already exist in `ops().current()`, verify that the current in-memory state is checked first and the refresh is performed only as a fallback.

## Code Smell

A call to `ops().refresh().snapshot(snapshotId)` appears unconditionally in `updateEvent()` with no prior attempt to read from `ops().current()`, meaning every successful commit triggers an extra metadata fetch even when the just-committed snapshot is already cached in the local `TableOperations` state.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: unconditional-refresh-before-cache-check
- severity: performance
- subsystem: core/snapshot-producer

## Root Cause

`MergingSnapshotProducer.updateEvent()` always fetched fresh metadata from the catalog via `ops().refresh()` to retrieve the newly committed snapshot, ignoring that `ops().current()` already holds the up-to-date metadata immediately after a successful commit. For REST catalog users this caused a redundant `GET /tables/{table}` request on every append, inflating catalog load and distorting commit metrics (the refreshed metadata could reflect an interleaved concurrent commit rather than the commit being reported).

## Example

```java
// BEFORE — always refreshes, discarding locally cached post-commit metadata
public Object updateEvent() {
    long snapshotId = snapshotId();
    Snapshot justSaved = ops().refresh().snapshot(snapshotId); // extra network call
    ...
}
```

```java
// AFTER — uses cached metadata; only refreshes if snapshot is absent
public Object updateEvent() {
    long snapshotId = snapshotId();
    Snapshot justSaved = ops().current().snapshot(snapshotId);
    if (justSaved == null) {
        justSaved = ops().refresh().snapshot(snapshotId);
    }
    ...
}
```
