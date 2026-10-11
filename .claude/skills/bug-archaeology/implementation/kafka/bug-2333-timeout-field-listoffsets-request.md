**Commit**: 560076ba9e · **Ticket**: KAFKA-15859
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This is part-3 of the KIP-1075. Added a `timeoutMs` field to the ListOffsets request. This timeout is applicable only for the topic/partitions that are enabled with remote storage. When the timeout is defined in the request, then we use it to define the delay timeout for `DelayedRemoteListOffsets` request.

**Before/After**
```java
// Before
        ListOffsetsHandler handler = new ListOffsetsHandler(offsetQueriesByPartition, options, logContext);
        LogContext logContext
                .setTargetTimes(new ArrayList<>(topicsByName.values()));
    private final long requestTimeoutMs;
                         long requestTimeoutMs,

// After
        ListOffsetsHandler handler = new ListOffsetsHandler(offsetQueriesByPartition, options, logContext, defaultApiTimeoutMs);
    private final int defaultApiTimeoutMs;
        LogContext logContext,
        int defaultApiTimeoutMs
        this.defaultApiTimeoutMs = defaultApiTimeoutMs;
        int timeoutMs = options.timeoutMs() != null ? options.timeoutMs() : defaultApiTimeoutMs;
                .setTargetTimes(new ArrayList<>(topicsByName.values()))
                .setTimeoutMs(timeoutMs);
    private final int requestTimeoutMs;
                         int requestTimeoutMs,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15859: add timeout field to the listoffsets request (#17112).

**Test**
Test modified in ListOffsetsHandlerTest.java covering the corrected behaviour.
