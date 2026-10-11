# Bug 2502: Hint deletion uses column timestamp instead of row max timestamp — hints not fully deleted

**Commit**: d5723b08bd · **Ticket**: CASSANDRA-3466

## Detection Rule

When deleting a hint row in the hints column family, using a single column's timestamp as the deletion timestamp instead of the row's `maxTimestamp()` will leave columns with higher timestamps un-deleted, causing hints to persist and be re-delivered.

## Code Smell

`deleteHint(tokenBytes, hint.name(), versionColumn.timestamp())` where `versionColumn` is a specific sub-column — using a single-column timestamp for a row deletion that must cover all sub-columns of that hint.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: partial-timestamp-for-row-deletion
- severity: data-loss
- subsystem: storage-engine

## Root Cause

`HintedHandOffManager.deliverHintsToEndpoint()` called `deleteHint(tokenBytes, hint.name(), versionColumn.timestamp())`, using only the `version` sub-column's timestamp as the deletion timestamp. If any sub-column of the hint had a higher timestamp, the deletion tombstone would not shadow those columns and the hint would remain in the hints CF, potentially causing duplicate delivery. The fix uses `hint.maxTimestamp()` to ensure the deletion covers all sub-columns.

## Example

```java
// BEFORE
if (sendMutation(endpoint, rm))
    deleteHint(tokenBytes, hint.name(), versionColumn.timestamp());  // may miss higher-ts columns
```

```java
// AFTER
if (sendMutation(endpoint, rm))
    deleteHint(tokenBytes, hint.name(), hint.maxTimestamp());  // covers all sub-columns
```
