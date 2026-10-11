**Commit**: a383a954c8 · **Ticket**: KAFKA-17808
**Subsystem**: connect/runtime/src/main/java/org/apache/kafka/connect/runtime/Worker.java
**Category**: Wrong constant / default value

**Root cause**: The admin client created for the dead-letter queue reporter was given a static client ID `"connector-dlq-adminclient-"` (missing the connector task ID suffix). Every DLQ admin client in every connector task therefore shared the same client ID, causing JMX metric collisions and confusing broker-side logging. The producer counterpart already correctly appended `+ id`; the admin client simply forgot to.

**Before/After**:
```java
// BEFORE – all DLQ admin clients share the same ID
Map<String, Object> adminProps = adminConfigs(id.connector(),
    "connector-dlq-adminclient-", ...);

// AFTER – unique ID per connector task
Map<String, Object> adminProps = adminConfigs(id.connector(),
    "connector-dlq-adminclient-" + id, ...);
```

**How to spot**: When a producer and its paired admin client are created in the same code block, compare the client-ID strings. A missing `+ id` on one but not the other is the classic typo for this class of bug.

**Test**: No test added. The fix is a one-character string concatenation correction.
