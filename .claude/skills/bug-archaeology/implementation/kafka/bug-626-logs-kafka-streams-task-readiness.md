**Commit**: 06c9213135 · **Ticket**: KAFKA-19682
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Add more debug/trace logs following the logic of how TaskExecutor
process task. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    abstract boolean readyToProcess(long wallClockTime);
    boolean readyToProcess(final long wallClockTime) {
            return true;


                logger.trace("Fetched lag for {} is {}", partition, fetchedLag);

                    logger.trace("Waiting to fetch data for {}", partition);
                    return false;
                    logger.trace(
                            "Lag for {} is currently {}, but no data is buffered locally. Waiting to buffer some records.",
                            partition,

// After
    /**
     * Result of readyToProcess check, containing both the readiness status
     * and an optional diagnostic log message.
     */
    static final class ReadyToProcessResult {
        private final boolean ready;
        private final Optional<String> logMessage;

        ReadyToProcessResult(final boolean ready, final Optional<String> logMessage) {
            if (ready && logMessage.isPresent()) {
                throw new IllegalArgumentException("Invalid ReadyToProcessResult: 'ready' is true but a log message is present.");
            } else if (!ready && logMessage.isEmpty()) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19682: add logs for kafka streams task readiness (#20693).

**Test**
Test modified in PartitionGroupTest.java covering the corrected behaviour.
