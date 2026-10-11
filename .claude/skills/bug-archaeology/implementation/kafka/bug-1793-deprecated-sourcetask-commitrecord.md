**Commit**: 0163fa2d06 · **Ticket**: KAFKA-18294
**Subsystem**: connect/api
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * <p>
     * Commit an individual {@link SourceRecord} when the callback from the producer client is received. This method is
     * also called when a record is filtered by a transformation, and thus will never be ACK'd by a broker.
     * <p>
     * This is an alias for {@link #commitRecord(SourceRecord, RecordMetadata)} for backwards compatibility. The default
     * implementation of {@link #commitRecord(SourceRecord, RecordMetadata)} just calls this method. It is not necessary
     * to override both methods.
     * <p>
     * SourceTasks are not required to implement this functionality; Kafka Connect will record offsets
     * automatically. This hook is provided for systems that also need to store offsets internally
     * in their own system.

// After
     * The default implementation is a nop. It is not necessary to implement the method.
        // by default, just do nothing
                if (metadata == null) {
                    block.maybeBlockOn(SOURCE_TASK_COMMIT_RECORD);
                } else {
                    block.maybeBlockOn(SOURCE_TASK_COMMIT_RECORD_WITH_METADATA);
                }
                        <li>The <code>commitRecord(SourceRecord)</code> method was removed from <code>SourceTask</code>.
                        </li>
```

**How to spot**
When reviewing connect/api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18294 remove deprecated sourcetask#commitrecord (#18260).

**Test**
Test modified in BlockingConnectorTest.java covering the corrected behaviour.
