**Commit**: f621a635c1 · **Ticket**: KAFKA-19570
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Offline migration essentially preserves offsets and nothing else. So
effectively write tombstones for classic group type when a streams
heartbeat is sent to with the group ID of an empty classic group, and
write tombstones for the streams group type when a classic consumer
attempts to join with a group ID of an empty streams group. Reviewers: Bill Bejeck <bbejeck@apache.org>, Sean Quah
 <squah@confluent.io>, Dongnuo Lyu <dlyu@confluent.io>

**Before/After**
```java
// Before
        String groupId
            group = getOrCreateStreamsGroup(groupId);
            } else if (group.type() == CONSUMER || group.type() == CLASSIC) {

// After
     * If there is an empty classic consumer group of the same name, it will be deleted and a new streams
     * group will be created.
     *
     * @param records           The record list to which the group tombstones are written
     *                          if the group is empty and is a classic group.
        String groupId,
        List<CoordinatorRecord> records
        } else if (maybeDeleteEmptyClassicGroup(group, records)) {
            log.info("[GroupId {}] Converted the empty classic group to a streams group.", groupId);
            return new StreamsGroup(logContext, snapshotRegistry, groupId, metrics);
            group = getOrCreateStreamsGroup(groupId, records);
            } else if (group.type() == CONSUMER || group.type() == CLASSIC || group.type() == STREAMS && group.isEmpty()) {
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19570: implement offline migration for streams groups (#20288).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
