**Commit**: 6e76736890 · **Ticket**: KAFKA-18827
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
* In this PR, we have provided implementation for the initialize share
group state RPC from the persister perspective. * Tests have been added wherever applicable. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import java.util.stream.Collectors;
        throw new RuntimeException("not implemented");

// After
import org.apache.kafka.common.requests.InitializeShareGroupStateResponse;
        try {
            validate(request);
        } catch (Exception e) {
            log.error("Unable to validate initialize state request", e);
            return CompletableFuture.failedFuture(e);
        }
        GroupTopicPartitionData<PartitionStateData> gtp = request.groupTopicPartitionData();
        String groupId = gtp.groupId();

        Map<Uuid, Map<Integer, CompletableFuture<InitializeShareGroupStateResponse>>> futureMap = new HashMap<>();
        List<PersisterStateManager.InitializeStateHandler> handlers = new ArrayList<>();
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18827: initialize share group state persister impl [2/n]. (#18992).

**Test**
Test modified in DefaultStatePersisterTest.java covering the corrected behaviour.
