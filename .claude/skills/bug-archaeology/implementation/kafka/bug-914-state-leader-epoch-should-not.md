**Commit**: 05b2601dde · **Ticket**: KAFKA-19456
**Subsystem**: share-coordinator/src
**Category**: State not cleaned up

**Root cause**
* If a write request with higher state than seen so far for a
specific share partition arrives at the share coordinator, the code will
create a new share snapshot and also update the internal view of the
state epoch. * For writes with higher leader epoch, the current records are updated
with that value as well. * The above is not the expected behavior and only initialize RPCs should
set and alter the state epoch and read RPC should set the leader epoch. * This PR rectifies the behavior.

**Before/After**
```java
// Before
        CoordinatorRecord record = generateShareStateRecord(partitionData, key);
        CoordinatorRecord record = generateShareStateRecord(writePartitionData, key);
     * @param partitionData - Represents the data which should be written into the share state record.
     * @param key           - The {@link SharePartitionKey} object.
        SharePartitionKey key
        if (snapshotUpdateCount.getOrDefault(key, 0) >= updatesPerSnapshotLimit || partitionData.stateEpoch() > shareStateMap.get(key).stateEpoch()) {
            ShareGroupOffset currentState = shareStateMap.get(key); // shareStateMap will have the entry as containsKey is true
            int newLeaderEpoch = partitionData.leaderEpoch() == -1 ? currentState.leaderEpoch() : partitionData.leaderEpoch();
            int newStateEpoch = partitionData.stateEpoch() == -1 ? currentState.stateEpoch() : partitionData.stateEpoch();
                    .setStateEpoch(newStateEpoch)
            ShareGroupOffset currentState = shareStateMap.get(key); // shareStateMap will have the entry as containsKey is true.


// After
        CoordinatorRecord record = generateShareStateRecord(partitionData, key, false);
        CoordinatorRecord record = generateShareStateRecord(writePartitionData, key, true);
     * @param partitionData     - Represents the data which should be written into the share state record.
     * @param key               - The {@link SharePartitionKey} object.
     * @param updateLeaderEpoch - Should the leader epoch be updated, if higher.
        SharePartitionKey key,
        boolean updateLeaderEpoch
        ShareGroupOffset currentState = shareStateMap.get(key); // This method assumes containsKey is true.

        int newLeaderEpoch = currentState.leaderEpoch();
        if (updateLeaderEpoch) {
            newLeaderEpoch = partitionData.leaderEpoch() != -1 ? partitionData.leaderEpoch() : newLeaderEpoch;
```

**How to spot**
When reviewing share-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19456: state and leader epoch should not be updated on writes. (#20079).

**Test**
Test modified in ShareCoordinatorShardTest.java covering the corrected behaviour.
