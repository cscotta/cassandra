**Commit**: 0bd1ff936f · **Ticket**: KAFKA-18629
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            this(data, false);
        throw new RuntimeException("not implemented");

// After
    <suppress checks="ClassFanOutComplexity"
              files="(PersisterStateManagerTest|DefaultStatePersisterTest).java"/>
            this(data, true);
import org.apache.kafka.common.requests.DeleteShareGroupStateResponse;
        try {
            validate(request);
        } catch (Exception e) {
            log.error("Unable to validate delete state request", e);
            return CompletableFuture.failedFuture(e);
        }
        GroupTopicPartitionData<PartitionIdData> gtp = request.groupTopicPartitionData();
        String groupId = gtp.groupId();
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18629: add persister impl and tests for deletesharegroupstate rpc. [2/n] (#18748).

**Test**
Test modified in DefaultStatePersisterTest.java covering the corrected behaviour.
