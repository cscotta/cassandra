**Commit**: ee550c4b77 · **Ticket**: KAFKA-16973
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When a write operation does not have any records, the coordinator runtime checked whether the state machine is caught-up to decide whether the operation should wait until the state machine is committed up to the operation point or the operation should be completed. The current implementation assumes that there will always be a pending write operation waiting in the deferred queue when the state machine is not fully caught-up yet. This is true except when the state machine is just loaded and not caught-up yet. This patch fixes the issue by always comparing the last written offset and the last committed offset.

**Before/After**
```java
// Before
import java.util.OptionalLong;
                    OptionalLong pendingOffset = deferredEventQueue.highestPendingOffset();
                    if (pendingOffset.isPresent()) {
                        deferredEventQueue.add(pendingOffset.getAsLong(), event);

// After
                    if (coordinator.lastCommittedOffset() < coordinator.lastWrittenOffset()) {
                        deferredEventQueue.add(coordinator.lastWrittenOffset(), event);
    @Test
    public void testWriteOpIsNotReleasedWhenStateMachineIsNotCaughtUpAfterLoad() throws ExecutionException, InterruptedException, TimeoutException {
        MockTimer timer = new MockTimer();
        MockPartitionWriter writer = new MockPartitionWriter();
        CoordinatorLoader<String> loader = new CoordinatorLoader<String>() {
            @Override
            public CompletableFuture<LoadSummary> load(
                TopicPartition tp,
                CoordinatorPlayback<String> coordinator
            ) {
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16973; fix caught-up condition (#16367).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
