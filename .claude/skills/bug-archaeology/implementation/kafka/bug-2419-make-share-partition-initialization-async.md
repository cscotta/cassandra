**Commit**: 4ac1dd4fc7 · **Ticket**: KAFKA-17482
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
The PR introduces states for share partition which are used to make share partition initilzation async. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.group.share.ReadShareGroupStateResult;
import java.util.concurrent.ExecutionException;
        // Initialize the partition.
        initialize();

// After
import org.apache.kafka.common.errors.LeaderNotAvailableException;
    /**
     * The SharePartitionState is used to track the state of the share partition. The state of the
     * share partition determines if the partition is ready to receive requests, be initialized with
     * persisted state, or has failed to initialize.
     */
    // Visible for testing
    enum SharePartitionState {
        /**
         * The share partition is empty and has not been initialized with persisted state.
         */
        EMPTY,
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17482: make share partition initialization async (kip-932) (#17097).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
