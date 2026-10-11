**Commit**: bb112570ae · **Ticket**: KAFKA-17109
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements exponential backoff for failed initializations of tasks due to lock exceptions. It increases the time between two consecutive attempts of initializing the tasks. Reviewer: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.ExponentialBackoff;
    private final HashMap<TaskId, BackoffRecord> lockedTasksToBackoffRecord = new HashMap<>();

                lockedTasksToBackoffRecord.remove(taskId);
                updateOrCreateBackoffRecord(taskId, System.currentTimeMillis());
            getOrCreateDirectoryForTask(taskId);
    public boolean canTryLock(final TaskId taskId, final long nowMs) {
        return !lockedTasksToBackoffRecord.containsKey(taskId) || lockedTasksToBackoffRecord.get(taskId).canAttempt(nowMs);
    }

    private void updateOrCreateBackoffRecord(final TaskId taskId, final long nowMs) {
        if (lockedTasksToBackoffRecord.containsKey(taskId)) {

// After
        if (!stateDirectory.lock(id)) {
        }
        log.debug("Acquired state directory lock");
        final boolean storeDirsEmpty = stateDirectory.directoryForTaskIsEmpty(id);
        stateMgr.registerStateStores(topology.stateStores(), processorContext);
        log.debug("Registered state stores");
        // We should only load checkpoint AFTER the corresponding state directory lock has been acquired and
        // the state stores have been registered;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17109: move lock backoff retry to streams taskmanager  (#17209).

**Test**
Test modified in StandbyTaskTest.java covering the corrected behaviour.
