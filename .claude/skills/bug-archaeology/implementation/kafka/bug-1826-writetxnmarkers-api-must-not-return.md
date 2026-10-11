**Commit**: 450c10d00c · **Ticket**: KAFKA-17507
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
We have observed the below errors in some cluster:

Uncaught exception in scheduled task 'handleTxnCompletion-902667' exception.message:Trying to complete a transactional offset commit for producerId *** and groupId *** even though the offset commit record itself hasn't been appended to the log. When a transaction is completed, the transaction coordinator sends a WriteTxnMarkers request to all the partitions involved in the transaction to write the markers to them. When the broker receives it, it writes the markers and if markers are written to the __consumer_offsets partitions, it informs the group coordinator that it can materialize the pending transactional offsets in its main cache. The ...

**Before/After**
```java
// Before
                                  transactionResult: TransactionResult): Unit = {
  ): Unit = {
    coordinator.scheduleHandleTxnCompletion(
      producerId,
      partitions.asScala,
      transactionResult
    )
import java.util.concurrent.ConcurrentHashMap
  def scheduleHandleTxnCompletion(producerId: Long, completedPartitions: Set[Int], isCommit: Boolean): Unit = {
    scheduler.scheduleOnce(s"handleTxnCompletion-$producerId", () =>
      handleTxnCompletion(producerId, completedPartitions, isCommit))

// After
import java.util.concurrent.CompletableFuture
                                  transactionResult: TransactionResult): CompletableFuture[Void] = {
  ): CompletableFuture[Void] = {
    try {
      coordinator.scheduleHandleTxnCompletion(
        producerId,
        partitions.asScala,
        transactionResult
      )
    } catch {
      case e: Throwable => FutureUtils.failedFuture(e)
    }
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17507; writetxnmarkers api must not return until markers are written and materialized in group coordinator's cache.

**Test**
Test modified in GroupCoordinatorAdapterTest.scala covering the corrected behaviour.
