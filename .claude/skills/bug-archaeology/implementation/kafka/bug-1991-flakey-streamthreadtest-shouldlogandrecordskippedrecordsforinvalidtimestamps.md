**Commit**: 2519e4af0c · **Ticket**: KAFKA-18038
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
With KAFKA-17872, we changed some internals that effects the conditions
of this test, introducing a race condition when the expected log
messages are printed. This PR adds additional wait-conditions to the test to close the race
condition. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
            mkEntry(InternalConfig.PROCESSING_THREADS_ENABLED, Boolean.toString(processingThreadsEnabled))
    public void shouldLogAndRecordSkippedRecordsForInvalidTimestamps(final boolean stateUpdaterEnabled, final boolean processingThreadsEnabled) {

// After
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import static org.apache.kafka.test.TestUtils.waitForCondition;
            mkEntry(InternalConfig.PROCESSING_THREADS_ENABLED, Boolean.toString(processingThreadsEnabled)),
            mkEntry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "1")
    public void shouldLogAndRecordSkippedRecordsForInvalidTimestamps(
        final boolean stateUpdaterEnabled,
        final boolean processingThreadsEnabled
    ) throws Exception {

            if (processingThreadsEnabled) {
                waitForCommit(mockConsumer, offset + 1);
            }
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18038: fix flakey test streamthreadtest.shouldlogandrecordskippedrecordsforinvalidtimestamps (#17889).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
