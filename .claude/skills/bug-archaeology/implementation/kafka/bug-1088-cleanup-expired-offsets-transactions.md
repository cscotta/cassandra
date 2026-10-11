## Commit

1293658cca — KAFKA-19163: cleanupExpiredOffsets deletes group with pending transactions

## Ticket

KAFKA-19163

## Subsystem

Group coordinator / offset metadata management

## Category

Logic error in condition

## Root cause

`OffsetMetadataManager.cleanupExpiredOffsets()` returned `true` (delete the group) when `offsetsByTopic` was `null` for the group, indicating no committed stable offsets existed. However, the check did not account for pending transactional offsets: a group could have `offsetsByTopic == null` and still have open transactions. Deleting the group in that state would silently discard the pending transactional offsets, causing the transaction to become uncommittable from the coordinator's perspective and leaving the producer in a stuck state.

## Before / After

```java
// Before — deletes group even when open transactions exist
if (offsetsByTopic == null) {
    return true;  // delete group
}

// After — only deletes if no open transactions reference this group
if (offsetsByTopic == null) {
    return !openTransactions.contains(groupId);
}
```

## How to spot

In `cleanupExpired*` methods that return a boolean "delete this entity" result, search for early-return `true` paths that do not check for pending or in-flight operations referencing the same entity. Any such path can silently discard in-progress state.

## Test

`OffsetMetadataManagerTest` was extended with a case where `offsetsByTopic` is null but an open transaction exists, asserting that the group is not deleted.
