**Commit**: 55d1e3823b · **Ticket**: KAFKA-20254
**Subsystem**: group-coordinator — GroupMetadataManager
**Category**: State not cleaned up

**Root cause**
During replay of the `__consumer_offsets` topic, offset-commit records can appear *before* streams-group metadata records (e.g., after log compaction removes the group-metadata tombstone). When `OffsetMetadataManager` replays an offset commit for a group that has no backing group object, it auto-creates a *simple* `ClassicGroup` to hold the offsets. When the streams-group records are subsequently replayed, `getOrMaybeCreatePersistedStreamsGroup` found the existing `ClassicGroup` and threw "Group X is not a streams group." The fix adds a branch that recognizes simple classic groups (which have no `GroupMetadataKey/Value` records and can be safely replaced) and replaces them with a new `StreamsGroup`.

**Before/After**
```java
// Before — no handling for simple ClassicGroup; throws on type mismatch
} else {
    throw new IllegalStateException("Group " + groupId + " is not a streams group");
}

// After — simple ClassicGroup safely replaced
} else if (group.type() == CLASSIC && ((ClassicGroup) group).isSimpleGroup()) {
    StreamsGroup streamsGroup = new StreamsGroup(logContext, snapshotRegistry, groupId);
    groups.put(groupId, streamsGroup);
    return streamsGroup;
} else {
    throw new IllegalStateException(...);
}
```

**How to spot**
The existing `getOrMaybeCreatePersistedConsumerGroup` already handled this pattern; `getOrMaybeCreatePersistedStreamsGroup` was written without it. When adding new group types, audit all `getOrMaybeCreatePersisted*` methods for consistent handling of simple classic groups that may have been auto-created during offset-commit replay.

**Test**
`GroupMetadataManagerTest#testReplayStreamsGroupMemberMetadataWithSimpleClassicGroup` — creates a simple classic group via `getOrMaybeCreateClassicGroup`, then replays a streams-group member record, and asserts the member is accessible from the resulting `StreamsGroup`.
