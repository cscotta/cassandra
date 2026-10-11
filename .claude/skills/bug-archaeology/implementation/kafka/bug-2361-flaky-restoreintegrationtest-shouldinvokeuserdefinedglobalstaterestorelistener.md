**Commit**: 8f5cf9968f · **Ticket**: KAFKA-17515
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Found two issues in the flaky tests: (Put the log analysis under Jira comments.)

1) The error "java.nio.file.DirectoryNotEmptyException" occurs if the flush() of kafkaStreams.close() and purgeLocalStreamsState() are triggered in the same time. (The current timeout is 5 sec, which is too short since the CI is unstable and slow). 2) Racing issue: Task to-be restored in ks-1 are rebalanced to ks-2 before entering active restoring state. So no onRestoreSuspend() was triggered.

**Before/After**
```java
// Before
        kafkaStreams.close(Duration.ofMillis(5000L));

// After
import static org.apache.kafka.streams.integration.utils.IntegrationTestUtils.waitForActiveRestoringTask;
        kafkaStreams.close();
        // Ensure all the restoring tasks are in active state before starting the new instance.
        // Otherwise, the tasks which assigned to first kafka streams won't encounter "restoring suspend" after being reassigned to the second instance.
        waitForActiveRestoringTask(kafkaStreams, 5, IntegrationTestUtils.DEFAULT_TIMEOUT);

    /**
     * Wait until enough restoring tasks have been started
     */
    public static void waitForActiveRestoringTask(final KafkaStreams streams,
                                                  final int expectedTasks,
                                                  final long timeoutMilliseconds) throws Exception {
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17515 fix flaky restoreintegrationtest.shouldinvokeuserdefinedglobalstaterestorelistener (#17187).

**Test**
Test modified in RestoreIntegrationTest.java covering the corrected behaviour.
