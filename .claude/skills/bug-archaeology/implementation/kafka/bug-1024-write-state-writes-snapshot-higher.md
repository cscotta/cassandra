**Commit**: 13b5627274 · **Ticket**: KAFKA-19337
**Subsystem**: share-coordinator/src
**Category**: State not cleaned up

**Root cause**
- Due to condition on number of updates/snapshot in
`generateShareStateRecord`, share updates gets written for write state
requests even if they have the highest state epoch seen so far. - A share update cannot record state epoch. As a result, this update
gets missed. - This PR remedies the issue and adds a test as proof of the fix.

**Before/After**
```java
// Before
    public static CoordinatorRecord newShareSnapshotUpdateRecord(String groupId, Uuid topicId, int partitionId, ShareGroupOffset offsetData) {
        } else if (snapshotUpdateCount.getOrDefault(key, 0) >= config.shareCoordinatorSnapshotUpdateRecordsPerSnapshot()) {
            return ShareCoordinatorRecordHelpers.newShareSnapshotUpdateRecord(
        CoordinatorRecord record = ShareCoordinatorRecordHelpers.newShareSnapshotUpdateRecord(

// After
    public static CoordinatorRecord newShareUpdateRecord(String groupId, Uuid topicId, int partitionId, ShareGroupOffset offsetData) {
        int updatesPerSnapshotLimit = config.shareCoordinatorSnapshotUpdateRecordsPerSnapshot();
        } else if (snapshotUpdateCount.getOrDefault(key, 0) >= updatesPerSnapshotLimit || partitionData.stateEpoch() > shareStateMap.get(key).stateEpoch()) {
            return ShareCoordinatorRecordHelpers.newShareUpdateRecord(
        CoordinatorRecord record = ShareCoordinatorRecordHelpers.newShareUpdateRecord(
    @Test
    public void testWriteStateSequentialRequestsWithHigherStateEpochCreateShareSnapshots() {
        // Makes 3 requests
```

**How to spot**
When reviewing share-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19337: write state writes snapshot for higher state epoch. (#19843).

**Test**
Test modified in ShareCoordinatorRecordHelpersTest.java covering the corrected behaviour.
