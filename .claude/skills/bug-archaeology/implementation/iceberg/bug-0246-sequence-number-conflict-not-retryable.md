# Bug 0246: Sequence number conflicts during concurrent commits are not retryable

**Commit**: 1d6c685c1391df604d59f3335a1fba7a1af2c25f · **Ticket**: #15126

## Detection Rule

When a server-side validation throws an exception because a commit contains stale values (sequence numbers, row IDs, or other monotonically-increasing counters that advance with each successful commit), verify the exception type allows the client to retry with refreshed metadata rather than treating the failure as a permanent conflict.

## Code Smell

A call to `ValidationException.check(snapshot.sequenceNumber() > lastSequenceNumber, ...)` in the path that applies metadata updates on the server; when this fires due to a concurrent commit on a different branch, the plain `ValidationException` propagates to the REST client as a non-retryable failure even though a client refresh-and-retry would succeed.

## Tags

- scope: distributed-systems
- category: logic-error
- code-pattern: non-retryable-transient-failure
- severity: crash
- subsystem: core/table-metadata, rest/catalog-handlers

## Root Cause

`TableMetadata.addSnapshot` threw a plain `ValidationException` when a snapshot's sequence number was not greater than `lastSequenceNumber` or when `firstRowId` was behind the table's `nextRowId`. Both conditions are caused by concurrent commits to other branches advancing the table's counters between when the client read metadata and when the server applied the update. Because `ValidationException` is treated as a permanent failure by the REST catalog, the client did not retry. Introducing `RetryableValidationException` (a subclass of `ValidationException`) and catching it in `CatalogHandlers` to re-wrap as `CommitFailedException` allows the client to refresh and retry the commit.

## Example

```java
// BEFORE
ValidationException.check(
    formatVersion == 1
        || snapshot.sequenceNumber() > lastSequenceNumber
        || snapshot.parentId() == null,
    "Cannot add snapshot with sequence number ...");
```

```java
// AFTER
RetryableValidationException.check(
    formatVersion == 1
        || snapshot.sequenceNumber() > lastSequenceNumber
        || snapshot.parentId() == null,
    "Cannot add snapshot with sequence number ...");
// CatalogHandlers catches RetryableValidationException and wraps as CommitFailedException
```
