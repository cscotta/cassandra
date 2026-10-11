**Commit**: faff2de6a5 · **Ticket**: KAFKA-17561
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Part of KIP-1091. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
        <td>kafka.streams:type=stream-thread-metrics,thread-id=([-.\w]+)</td>
            name, THREAD_LEVEL_GROUP, description, threadLevelTagMap(threadId));
        final Map<String, String> tagMap = new LinkedHashMap<>();
        final Map<String, String> tagMap = threadLevelTagMap(threadId);
                                               final String taskName,
        final Map<String, String> tagMap = taskLevelTagMap(threadId, taskName);
                                                final String taskName,

// After
        <td>kafka.streams:type=stream-thread-metrics,thread-id=([-.\w]+),process-id=([-.\w]+)</td>
            processId,
                        final UUID processId,
            processId.toString(),
        addThreadLevelMutableMetric(name, description, threadId, Collections.emptyMap(), valueProvider);
    }
    public <T> void addThreadLevelMutableMetric(final String name,
                                                final String description,
                                                final String threadId,
                                                final Map<String, String> additionalTags,
                                                final Gauge<T> valueProvider) {
            name, THREAD_LEVEL_GROUP, description, threadLevelTagMap(threadId, additionalTags));
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17561: add processid tag to thread-state metric (#18581).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
