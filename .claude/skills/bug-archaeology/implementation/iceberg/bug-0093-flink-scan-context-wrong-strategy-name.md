# Bug 0093: Flink ScanContext error messages reference wrong strategy name constants

**Commit**: 0ed7f7791f5179a924f8c7772428587c1d8cbdfe · **Ticket**: #15907

## Detection Rule

When a `Preconditions.checkArgument` (or similar guard) error message includes a hardcoded string representation of an enum value, verify that the message embeds the actual enum instance (e.g., via `%s` formatting) rather than a manually typed name, because the literal string can diverge from the real enum name during renaming or copy-paste.

## Code Smell

A `Preconditions.checkArgument(condition, "... for SPECIFIC_START_SNAPSHOT_ID strategy ...")` message string that contains a different strategy name than the enum value being checked in the enclosing `if` block.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: hardcoded-enum-name
- severity: cosmetic
- subsystem: flink/ScanContext

## Root Cause

The error messages inside the `INCREMENTAL_FROM_SNAPSHOT_ID` and `INCREMENTAL_FROM_SNAPSHOT_TIMESTAMP` branches were copied from an earlier revision and still referenced the old enum names `SPECIFIC_START_SNAPSHOT_ID` and `SPECIFIC_START_SNAPSHOT_TIMESTAMP`. When a validation error was thrown, users saw a strategy name that did not correspond to the strategy they had configured, making diagnosis harder. The fix embeds the enum value via `%s` formatting so the message is always accurate.

## Example

```java
// BEFORE — message says "SPECIFIC_START_SNAPSHOT_ID" when strategy is INCREMENTAL_FROM_SNAPSHOT_ID
if (startingStrategy == StreamingStartingStrategy.INCREMENTAL_FROM_SNAPSHOT_ID) {
    Preconditions.checkArgument(
        startSnapshotId != null,
        "Invalid starting snapshot id for SPECIFIC_START_SNAPSHOT_ID strategy: null");
}
```

```java
// AFTER
if (startingStrategy == StreamingStartingStrategy.INCREMENTAL_FROM_SNAPSHOT_ID) {
    Preconditions.checkArgument(
        startSnapshotId != null,
        "Invalid starting snapshot id for %s strategy: null",
        startingStrategy);
}
```
