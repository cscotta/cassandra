**Commit**: 89418b66ae · **Ticket**: KAFKA-17442
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The PR makes the persister write RPC async. Also handles the errors from persister as per the review comment here:
Addressing review comment for PR: #16397 (comment)

Reviewers: Andrew Schofield <aschofield@confluent.io>, Abhinav Dixit <adixit@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.group.share.WriteShareGroupStateResult;
     * @param memberId The member id of the client that is fetching the record.
     *
     * @param memberId The member id of the client that is fetching the record.
     *
    public CompletableFuture<Optional<Throwable>> acknowledge(
        lock.writeLock().lock();
            rollbackOrProcessStateUpdates(throwable, updatedStates, stateBatches);
        return CompletableFuture.completedFuture(Optional.ofNullable(throwable));
     *
    public CompletableFuture<Optional<Throwable>> releaseAcquiredRecords(String memberId) {
        lock.writeLock().lock();

// After
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.CoordinatorNotAvailableException;
import org.apache.kafka.common.errors.FencedStateEpochException;
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;
import org.apache.kafka.common.errors.UnknownServerException;
     * @param memberId           The member id of the client that is fetching the record.
     * @param memberId               The member id of the client that is fetching the record.
    public CompletableFuture<Void> acknowledge(
        CompletableFuture<Void> future = new CompletableFuture<>();
        lock.writeLock().lock();
            rollbackOrProcessStateUpdates(future, throwable, updatedStates, stateBatches);
        return future;
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17442: handled persister errors with write async calls (kip-932) (#16956).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
