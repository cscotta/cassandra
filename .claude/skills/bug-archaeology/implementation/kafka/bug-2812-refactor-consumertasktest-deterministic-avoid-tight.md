**Commit**: 64702bcf6f · **Ticket**: KAFKA-16862
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
            try {
                if (hasAssignmentChanged) {
                    maybeWaitForPartitionAssignments();
                }
                log.trace("Polling consumer to receive remote log metadata topic records");
                final ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(Duration.ofMillis(pollTimeoutMs));
                for (ConsumerRecord<byte[], byte[]> record : consumerRecords) {
                    processConsumerRecord(record);
                }
                maybeMarkUserPartitionsAsReady();
            } catch (final WakeupException ex) {
                // ignore logging the error

// After
            ingestRecords();
        }
        closeConsumer();
        log.info("Exited from consumer task thread");
    }
    // public for testing
    public void ingestRecords() {
        try {
            if (hasAssignmentChanged) {
                maybeWaitForPartitionAssignments();
            }

```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16862: refactor consumertasktest to be deterministic and avoid tight loops (#16303).

**Test**
Test modified in ConsumerTaskTest.java covering the corrected behaviour.
