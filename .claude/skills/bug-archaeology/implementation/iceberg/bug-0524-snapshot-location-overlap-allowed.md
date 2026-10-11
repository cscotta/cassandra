# Bug 0524: Snapshot table action allowed overlapping locations, mixing source and destination files

**Commit**: 2c92500e974eb5a58cdcf75126a85d7cf16d2995 · **Ticket**: #14933

## Detection Rule

When a table creation or migration action accepts a user-supplied destination location, verify that the code validates the destination does not overlap (equal, contain, or be contained by) the source table location before proceeding.

## Code Smell

A `TODO` comment placeholder saying "Check the dest table location does not overlap with the source table location" with no actual validation code, allowing the action to proceed regardless of location overlap.

## Tags

- scope: project-specific
- category: missing-validation
- code-pattern: missing-precondition-check
- severity: data-loss
- subsystem: spark/actions/SnapshotTableSparkAction

## Root Cause

The `SnapshotTableSparkAction.execute()` method had a `TODO` comment marking a known missing safety check for destination location overlap with the source table. Without this check, a user could snapshot a table to the same location or a parent/child directory, causing Iceberg metadata files to be written into the source table's directory and mixing the two tables' files together. All three overlap cases (equal, dest is subdir of source, source is subdir of dest) needed to be rejected.

## Example

```java
// BEFORE
StagedSparkTable stagedTable = stageDestTable();
Table icebergTable = stagedTable.table();

// TODO: Check the dest table location does not overlap with the source table location

boolean threw = true;
```

```java
// AFTER
StagedSparkTable stagedTable = stageDestTable();
Table icebergTable = stagedTable.table();

String sourceTableLocation = sourceTableLocation();
String stagedTableLocation = icebergTable.location();
Preconditions.checkArgument(
    !sourceTableLocation.equals(stagedTableLocation)
        && !stagedTableLocation.startsWith(sourceTableLocation + "/")
        && !sourceTableLocation.startsWith(stagedTableLocation + "/"),
    "Cannot create a snapshot at location %s because it would overlap with source table location %s. "
        + "Overlapping snapshot and source would mix table files.",
    stagedTableLocation,
    sourceTableLocation);

boolean threw = true;
```
