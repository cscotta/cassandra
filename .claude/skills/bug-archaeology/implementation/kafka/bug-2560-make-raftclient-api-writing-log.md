**Commit**: 0f7cd4dcde · **Ticket**: KAFKA-17304
**Subsystem**: metadata/src
**Category**: Race condition / missing synchronization

**Root cause**
RaftClient API is changed to separate the batch accumulation (RaftClient#prepareAppend) from scheduling the append of accumulated batches (RaftClient#schedulePrepatedAppend) to the KRaft log. This change is needed to better match the controller's flow of replaying the generated records before replicating them. When the controller replay records it needs to know the offset associated with the record. To compute a table offset the KafkaClient needs to be aware of the records and their log position.

**Before/After**
```java
// Before
        val offset = raftManager.client.scheduleAppend(leaderEpoch, List(payload).asJava)
     * @param endOffset The offset of the last record that was written.
    void handleScheduleAtomicAppend(long endOffset) {
        this.nextWriteOffset = endOffset + 1;
        snapshotRegistry.idempotentCreateSnapshot(endOffset);
        metrics.setLastAppliedRecordOffset(endOffset);
                        long nextWriteOffset = offsetControl.nextWriteOffset();
                            long recordOffset = nextWriteOffset + recordIndex;
                                    recordOffset, nextWriteOffset);
                        long nextEndOffset = nextWriteOffset - 1 + recordIndex;
                        raftClient.scheduleAtomicAppend(controllerEpoch,
                            OptionalLong.of(nextWriteOffset),

// After
        val offset = raftManager.client.prepareAppend(leaderEpoch, List(payload).asJava)
        raftManager.client.schedulePreparedAppend()
     * @param lastOffset The offset of the last record that was written.
    void handleScheduleAppend(long lastOffset) {
        this.nextWriteOffset = lastOffset + 1;
        snapshotRegistry.idempotentCreateSnapshot(lastOffset);
        metrics.setLastAppliedRecordOffset(lastOffset);
                        long lastOffset = raftClient.prepareAppend(controllerEpoch, records);
                        long baseOffset = lastOffset - records.size() + 1;
                            long recordOffset = baseOffset + recordIndex;
                                    recordOffset, baseOffset);
                        raftClient.s
```

**How to spot**
When reviewing metadata/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17304; make raftclient api for writing to log explicit (#16862).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
