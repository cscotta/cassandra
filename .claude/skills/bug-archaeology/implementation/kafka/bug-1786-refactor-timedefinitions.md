**Commit**: fe9847dd4a · **Ticket**: KAFKA-17131
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Refactor TimeDefintiions to not use old ProcessorContext any longer. Reviewers: Bruno Cadonna <bruno@confluent.io>

**Before/After**
```java
// Before
         * As a result, the buffer will consume as much memory as it needs, dictated by the time bound.
         * If there isn't enough heap available to meet the demand, the application will encounter an
         * This is a convenient option if you doubt that your buffer will be that large, but also don't
         * This buffer is "strict" in the sense that it will enforce the time bound or crash.
         * As a result, the buffer will consume as much memory as it needs, dictated by the time bound.
         * If there isn't enough heap available to meet the demand, the application will encounter an
         * This is a convenient option if you doubt that your buffer will be that large, but also don't
         * This buffer is "strict" in the sense that it will enforce the time bound or crash.
         * This buffer is "strict" in the sense that it will enforce the time bound or shut down.
         * This buffer is "not strict" in the sense that it may emit early, so it is suitable for reducing
     * By default, all Streams operators emit results whenever new results are available.
     * This configuration will instead emit just one result per key for each window, guaranteeing

// After
         * <p>As a result, the buffer will consume as much memory as it needs, dictated by the time bound.
         * <p>If there isn't enough heap available to meet the demand, the application will encounter an
         * <p>This is a convenient option if you doubt that your buffer will be that large, but also don't
         * <p>This buffer is "strict" in the sense that it will enforce the time bound or crash.
         * <p>As a result, the buffer will consume as much memory as it needs, dictated by the time bound.
         * <p>If there isn't enough heap available to meet the demand, the application will encounter an
         * <p>This is a convenient option if you doubt that your buffer will be that large, but also don't
         * <p>This buffer is "strict" in the sense that it will enforce the time bound or crash.
         * <p>This buffer is "strict" in the sense that it will enforce the time bound or shut down.
         * <p>This buffer is "not strict" in the sense that it may emit early, so it is suitable for reducing
         *
     * <p>By default, all Streams operators emit results whenever new results are available.
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17131: refactor timedefinitions (#18241).

**Test**
No test added.
