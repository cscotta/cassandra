**Commit**: ab8ef87c7f · **Ticket**: KAFKA-18654
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-18575 solved a critical race condition by returning with CONCURRENT_TRANSACTIONS early when the transaction was still completing. In testing, it was discovered that this early return could cause performance regressions. Prior to KIP-890 the addpartitions call was a separate call from the producer. There was a previous change https://issues.apache.org/jira/browse/KAFKA-5477 that decreased the retry backoff to 20ms.

**Before/After**
```java
// Before
  def maybeStartTransactionVerification(producerId: Long, sequence: Int, epoch: Short): VerificationGuard = {
      case Some(log) => log.maybeStartTransactionVerification(producerId, sequence, epoch)
    replicaManager.maybeStartTransactionVerificationForPartition(
        groupManager.replicaManager.maybeStartTransactionVerificationForPartition(
  def maybeStartTransactionVerification(producerId: Long, sequence: Int, epoch: Short): VerificationGuard = lock synchronized {
      maybeCreateVerificationGuard(producerId, sequence, epoch)
                                           epoch: Short): VerificationGuard = lock synchronized {
    producerStateManager.maybeCreateVerificationStateEntry(producerId, sequence, epoch).verificationGuard

// After
  def maybeStartTransactionVerification(producerId: Long, sequence: Int, epoch: Short, supportsEpochBump: Boolean): VerificationGuard = {
      case Some(log) => log.maybeStartTransactionVerification(producerId, sequence, epoch, supportsEpochBump)
    replicaManager.maybeSendPartitionToTransactionCoordinator(
        groupManager.replicaManager.maybeSendPartitionToTransactionCoordinator(
  def maybeStartTransactionVerification(producerId: Long, sequence: Int, epoch: Short, supportsEpochBump: Boolean): VerificationGuard = lock synchronized {
      maybeCreateVerificationGuard(producerId, sequence, epoch, supportsEpochBump)
                                           epoch: Short,
                                           supportsEpochBump: Boolean): VerificationGuard = lock synchronized {
    producerStateManager.maybeCreate
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18654 [1/2]: transaction version 2 performance regression due to early return (#18720).

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
