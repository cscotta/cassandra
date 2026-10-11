**Commit**: 917d695322 · **Ticket**: KAFKA-17019

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/producer/KafkaProducer.java

**Category**: Missing null/bounds check

**Root cause**
`KafkaProducer` threw `TimeoutException` in multiple transactional paths (`initTransactions`, `sendOffsetsToTransaction`, `commitTransaction`, `abortTransaction`) without any contextual cause message, leaving users with no diagnostic information beyond the bare exception type. Since each timeout occurs for a distinct operational reason (coordinator discovery vs. response wait), the fix introduces static `PotentialCauseException` messages for each call site and threads them through the `TransactionalRequestResult.await()` overload that accepts a hint string.

**Before**
```java
// Before: no hint message
result.await(maxBlockTimeMs, TimeUnit.MILLISECONDS);
```

**After**
```java
// After: includes contextual hint
result.await(maxBlockTimeMs, TimeUnit.MILLISECONDS,
    INIT_TXN_TIMEOUT_MSG);
```

**How to spot**
Search for `result.await(maxBlockTimeMs, TimeUnit.MILLISECONDS)` calls in `KafkaProducer` that do not pass a third argument; any such bare call provides no root-cause hint to the user when a timeout occurs.

**Test**
Simulate a timeout in `initTransactions` and assert that the thrown `TimeoutException` message contains the expected coordinator-discovery hint text.
