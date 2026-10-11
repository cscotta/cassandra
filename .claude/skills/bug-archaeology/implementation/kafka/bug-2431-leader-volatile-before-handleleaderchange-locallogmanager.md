**Commit**: 1fd1646eb9 · **Ticket**: KAFKA-15648
**Subsystem**: metadata/src
**Category**: Race condition / missing synchronization

**Root cause**
Update the leader before calling handleLeaderChange and use the given epoch in LocalLogManager#prepareAppend. This should hopefully fix several flaky QuorumControllerTest tests. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
                                listenerData.handleLeaderChange(entryOffset, batch.newLeader);
        return shared.tryAppend(nodeId, leader.epoch(), batch);

// After
                                listenerData.handleLeaderChange(entryOffset, batch.newLeader);
        return shared.tryAppend(nodeId, epoch, batch);
```

**How to spot**
When reviewing metadata/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15648 update leader volatile before handleleaderchange in locallogmanager (#17118).

**Test**
Test modified in LocalLogManager.java covering the corrected behaviour.
