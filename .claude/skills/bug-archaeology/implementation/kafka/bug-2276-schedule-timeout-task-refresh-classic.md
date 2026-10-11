**Commit**: cbc02e006d · **Ticket**: KAFKA-16106
**Subsystem**: group-coordinator/src
**Category**: Wrong constant / default value

**Root cause**
In the existing implementation, If an operation modifying the classic group state fails, the group reverts but the group size counter does not. This creates an inconsistency between the group size metric and the actual group size. Considering that It will be complicated to rely on the appendFuture to revert the metrics upon the operation failure, this PR introduces a new implementation. A timeout task will periodically refresh the metrics based on the current groups soft state.

**Before/After**
```java
// Before
            metrics.onClassicGroupStateTransition(EMPTY, null);
            ClassicGroup classicGroup = new ClassicGroup(logContext, groupId, ClassicGroupState.EMPTY, time, metrics);
            metrics.onClassicGroupStateTransition(null, classicGroup.currentState());
                metrics,
        metrics.onClassicGroupStateTransition(null, classicGroup.currentState());
        CompletableFuture<Void> appendFuture = new CompletableFuture<>();
        appendFuture.exceptionally

// After
    /**
     * The classic group size counter key to schedule a timer task.
     *
     * Visible for testing.
     */
    static final String CLASSIC_GROUP_SIZE_COUNTER_KEY = "classic-group-size-counter";

    /**
     * Hardcoded default value of the interval to update the classic group size counter.
     */
    static final int DEFAULT_GROUP_GAUGES_UPDATE_INTERVAL_MS = 60 * 1000;

```

**How to spot**
When reviewing group-coordinator/src code, look for wrong constant / default value patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16106; schedule timeout task to refresh classic group size metric (#17325).

**Test**
Test modified in GroupCoordinatorRecordHelpersTest.java covering the corrected behaviour.
