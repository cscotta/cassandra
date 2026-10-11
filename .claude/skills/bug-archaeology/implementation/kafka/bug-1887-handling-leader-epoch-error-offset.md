**Commit**: ccca9f146e · **Ticket**: KAFKA-17945
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
The PR sends known leader epoch while fetch offset information. If API throws exception then PR adds way to handle the exceptions and surface same to make decision for handling share partition itself. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
    private final SharePartitionManager sharePartitionManager;
            SharePartitionManager sharePartitionManager,
        this.sharePartitionManager = sharePartitionManager;
                sharePartitions, replicaManager));
                sharePartitionManager.handleFencedSharePartitionException(
        topicIdPartitions.forEach(topicIdPartition -> sharePartitionManager.handleFencedSharePartitionException(
            ReplicaManager replicaManager
    ) {

// After
import java.util.function.BiConsumer;
    private final BiConsumer<SharePartitionKey, Throwable> exceptionHandler;
            BiConsumer<SharePartitionKey, Throwable> exceptionHandler,
        this.exceptionHandler = exceptionHandler;
                sharePartitions, replicaManager, exceptionHandler));
                exceptionHandler.accept(
        topicIdPartitions.forEach(topicIdPartition -> exceptionHandler.accept(
import org.apache.kafka.server.share.SharePartitionKey;
import java.util.function.BiConsumer;
            ReplicaManager replicaManager,
            BiConsumer<SharePartitionKey, Throwable> exceptionHandler) {
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17945, kafka-17944: handling leader epoch error for offset reset apis (#17979).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
