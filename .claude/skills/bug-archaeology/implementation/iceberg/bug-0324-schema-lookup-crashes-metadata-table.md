# Bug 0324: SnapshotUtil.schemaFor crashes when called on a metadata table

**Commit**: 6a41168d1aede8597eff057d91681ccd0eafbec1 · **Ticket**: #15387

## Detection Rule

When a utility method accepts a `Table` parameter and then calls `table.snapshot(snapshotId)` expecting a non-null result, verify that the code handles subclasses (e.g., metadata tables) that do not store snapshots in the same way as regular tables.

## Code Smell

A method calls `table.snapshot(snapshotId)` and immediately passes the result to `Preconditions.checkArgument(snapshot != null, ...)` without first checking whether the `Table` instance is a metadata table type whose snapshot model differs from a regular data table.

## Tags

- scope: project-specific
- category: missing-null-check
- code-pattern: subclass-not-handled-before-lookup
- severity: crash
- subsystem: core/snapshot-util

## Root Cause

`SnapshotUtil.schemaFor(Table, long)` unconditionally called `table.snapshot(snapshotId)` to retrieve the snapshot's `schemaId`. For `BaseMetadataTable` instances, snapshots are not stored as regular snapshots, so the lookup returns null and the `Preconditions.checkArgument` fires an `IllegalArgumentException`. Metadata tables support time travel but derive their schema from the underlying data table, not from individual snapshots.

## Example

```java
// BEFORE
public static Schema schemaFor(Table table, long snapshotId) {
    Snapshot snapshot = table.snapshot(snapshotId);
    Preconditions.checkArgument(snapshot != null, "Cannot find snapshot with ID %s", snapshotId);
    Integer schemaId = snapshot.schemaId();
    // ...
}
```

```java
// AFTER
public static Schema schemaFor(Table table, long snapshotId) {
    if (table instanceof BaseMetadataTable) {
        return table.schema();
    }
    Snapshot snapshot = table.snapshot(snapshotId);
    Preconditions.checkArgument(snapshot != null, "Cannot find snapshot with ID %s", snapshotId);
    Integer schemaId = snapshot.schemaId();
    // ...
}
```
