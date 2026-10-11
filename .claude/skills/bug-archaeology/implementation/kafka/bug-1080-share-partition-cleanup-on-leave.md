## Commit

ec70c44362 — KAFKA-19116/KAFKA-19258: Share partitions and sessions not cleaned up on group member leave

## Ticket

KAFKA-19116, KAFKA-19258

## Subsystem

Share groups / share partition manager

## Category

State not cleaned up

## Root cause

When a share group member left the group or the group became empty, `SharePartitionManager` and its session cache were never notified. The share partitions (in-memory state tracking acknowledgement cursors) and share sessions (network-level session objects) continued to exist and consume memory indefinitely. Under churn — frequent consumer joins and leaves — the share partition cache could grow without bound. The fix implements `ShareGroupListener` and registers a `ShareGroupListenerImpl` with the group coordinator; the listener triggers `SharePartitionManager.cleanupShareGroupState(groupId)` on member-leave and group-empty events.

## Before / After

```java
// Before — no cleanup on group leave
// GroupMetadataManager: member leave is handled but no ShareGroupListener notified

// After — listener triggers cache cleanup
class ShareGroupListenerImpl implements ShareGroupListener {
    @Override
    public void onShareGroupEmpty(String groupId) {
        sharePartitionManager.cleanupShareGroupState(groupId);
    }
    @Override
    public void onMemberLeave(String groupId, String memberId) {
        sharePartitionManager.releasePartitionsForMember(groupId, memberId);
    }
}
```

## How to spot

Search for group-coordinator member-leave and group-empty callbacks. Any subsystem that maintains per-group or per-member state (session caches, partition locks, cursor maps) must register a listener on these events or it will accumulate stale state indefinitely.

## Test

`SharePartitionManagerTest` was extended with tests that verify that partition and session entries are removed after a member-leave event and after the group becomes empty.
