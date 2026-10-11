## Bug 513 — Docs: Admin#createPartitions missing InvalidPartitionsException javadoc

**Commit:** 380cda94c1
**Ticket:** KAFKA-20021
**Subsystem:** clients / admin API docs
**Category:** Logic error in condition

### Root cause
The `Admin#createPartitions` API documentation did not mention that `InvalidPartitionsException` is thrown when the new partition count is less than or equal to the current count. Callers had no way to know from the API contract alone which exception to catch for this common error case, leading to uncaught runtime exceptions in production code.

### Before
```java
/** … @throws … (no mention of InvalidPartitionsException) … */
Map<String, KafkaFuture<Void>> createPartitions(…);
```

### After
```java
/**
 * …
 * @throws InvalidPartitionsException if the requested partition count ≤ current count
 * …
 */
```

### How to spot
Public API methods that throw documented exceptions internally but do not list them in the Javadoc `@throws` clause leave callers without guidance. Review the implementation for `throw new …Exception(…)` calls and ensure they are all surfaced in the contract.

### Test
Documentation-only change; no automated test.
