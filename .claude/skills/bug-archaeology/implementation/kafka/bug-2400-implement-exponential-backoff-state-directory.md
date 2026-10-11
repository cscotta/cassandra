**Commit**: d04f534892 · **Ticket**: KAFKA-17109
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements exponential backoff for state directory lock to increase the time between two consecutive attempts of acquiring the lock. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
        if (!stateDirectory.lock(id)) {
        }
        log.debug("Acquired state directory lock");
        final boolean storeDirsEmpty = stateDirectory.directoryForTaskIsEmpty(id);
        stateMgr.registerStateStores(topology.stateStores(), processorContext);
        log.debug("Registered state stores");
        // We should only load checkpoint AFTER the corresponding state directory lock has been acquired and
        // the state stores have been registered; we should not try to load at the state manager construction time.
        // See https://issues.apache.org/jira/browse/KAFKA-8574
        stateMgr.initializeStoreOffsetsFromCheckpoint(storeDirsEmpty);
        log.debug("Initialized state stores");

// After
import org.apache.kafka.common.utils.ExponentialBackoff;
    private final HashMap<TaskId, BackoffRecord> lockedTasksToBackoffRecord = new HashMap<>();

                lockedTasksToBackoffRecord.remove(taskId);
                updateOrCreateBackoffRecord(taskId, System.currentTimeMillis());
    public boolean canTryLock(final TaskId taskId, final long nowMs) {
        return !lockedTasksToBackoffRecord.containsKey(taskId) || lockedTasksToBackoffRecord.get(taskId).canAttempt(nowMs);
    }

    private void updateOrCreateBackoffRecord(final TaskId taskId, final long nowMs) {
        if (lockedTasksToBackoffRecord.containsKey(taskId)) {
            lockedTasksToBackoffRecord.get(taskId).recordAttempt(nowMs);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17109: implement exponential backoff for state directory lock (#17116).

**Test**
Test modified in StandbyTaskTest.java covering the corrected behaviour.
