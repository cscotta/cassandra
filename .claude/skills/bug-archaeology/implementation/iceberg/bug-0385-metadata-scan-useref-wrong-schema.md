# Bug 0385: Metadata table scans ignore branch snapshot when useRef overrides schema

**Commit**: c75efa9c32d85bc1c804a3909a28048fc70c6833 · **Ticket**: #15276

## Detection Rule

When a scan method accepts a named ref (branch/tag) and both updates the snapshot ID and selects a schema, verify that metadata table implementations that need the fixed metadata schema are not inadvertently replaced with a data-snapshot schema derived from the ref.

## Code Smell

A `useRef()` override in a base scan class that unconditionally calls `SnapshotUtil.schemaFor(table, name)` before delegating to `newRefinedScan()`, so subclasses (e.g. metadata tables) that override `tableSchema()` to return a fixed synthetic schema always receive the wrong schema.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: core/metadata-tables

## Root Cause

`SnapshotScan.useRef()` unconditionally passed `SnapshotUtil.schemaFor(table, name)` (the data schema at the snapshot) as the schema for the refined scan. Metadata table scans override `tableSchema()` to return their own fixed virtual schema, but `useRef()` bypassed this by always picking the data snapshot's schema, causing metadata columns to disappear and filter pushdown to fail.

## Example

```java
// BEFORE
public ThisT useRef(String name) {
    Snapshot snapshot = table().snapshot(name);
    TableScanContext newContext = context().useSnapshotId(snapshot.snapshotId());
    return newRefinedScan(table(), SnapshotUtil.schemaFor(table(), name), newContext);
}
```

```java
// AFTER
public ThisT useRef(String name) {
    Snapshot snapshot = table().snapshot(name);
    TableScanContext newContext = context().useSnapshotId(snapshot.snapshotId());
    Schema newSchema = useSnapshotSchema() ? SnapshotUtil.schemaFor(table(), name) : tableSchema();
    return newRefinedScan(table(), newSchema, newContext);
}
```
