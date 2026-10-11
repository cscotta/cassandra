**Commit**: 1a106e4538 · **Ticket**: KAFKA-18655
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
During testing we discovered that the empty group count is not updated in group conversion, but when the new group is transition to other state, the empty group count is decremented. This could result in negative empty group count. We can have a new consumer group count implementation that follows the pattern we did for the classic group count. The timeout task periodically refreshes the metrics based on the current groups soft state.

**Before/After**
```java
// Before
     * The classic group size counter key to schedule a timer task.
    static final String CLASSIC_GROUP_SIZE_COUNTER_KEY = "classic-group-size-counter";
     * Schedules (or reschedules) the group size counter for the classic groups.
    private void scheduleClassicGroupSizeCounter() {
            CLASSIC_GROUP_SIZE_COUNTER_KEY,
                groupMetadataManager.updateClassicGroupSizeCounter();
                scheduleClassicGroupSizeCounter();
     * Cancels the group size counter for the classic groups.
    private void cancelClassicGroupSizeCounter() {
        timer.cancel(CLASSIC_GROUP_SIZE_COUNTER_KEY);
        scheduleClassicGroupSizeCounter();
        cancelClassicGroupSizeCounter();

// After
     * The classic and consumer group size counter key to schedule a timer task.
    static final String GROUP_SIZE_COUNTER_KEY = "group-size-counter";
     * Schedules (or reschedules) the group size counter for the classic/consumer groups.
    private void scheduleGroupSizeCounter() {
            GROUP_SIZE_COUNTER_KEY,
                groupMetadataManager.updateGroupSizeCounter();
                scheduleGroupSizeCounter();
     * Cancels the group size counter for the classic/consumer groups.
    private void cancelGroupSizeCounter() {
        timer.cancel(GROUP_SIZE_COUNTER_KEY);
        scheduleGroupSizeCounter();
        cancelGroupSizeCounter();
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18655: implement the consumer group size counter with scheduled task (#18717).

**Test**
Test modified in GroupCoordinatorShardTest.java covering the corrected behaviour.
