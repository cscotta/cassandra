**Commit**: ba7b5c9b32 · **Ticket**: KAFKA-19367
**Subsystem**: core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala
**Category**: State not cleaned up

**Root cause**: When an epoch-fence transition write to the transaction log failed (e.g., `COORDINATOR_NOT_AVAILABLE`) under Transaction Version 2 (TV2), the coordinator incorrectly set `hasFailedEpochFence = true` on the producer's `TransactionMetadata`. In TV2 the epoch bump is not committed during a failed write, so the flag was wrong and blocked future epoch bumps on retry — the client would see repeated errors instead of the expected single-bump-then-success flow.

**Before/After**:
```scala
// BEFORE – incorrectly marks epoch fence as failed for TV2
case Some(epochAndMetadata) =>
  if (epochAndMetadata.coordinatorEpoch == coordinatorEpoch) {
    epochAndMetadata.transactionMetadata.hasFailedEpochFence = true
    warn(s"...epoch was increased to ${newMetadata.producerEpoch} but not returned to the client")
  }

// AFTER – TV2 allows re-bumping; do not set the flag
case Some(epochAndMetadata) =>
  if (epochAndMetadata.coordinatorEpoch == coordinatorEpoch) {
    // For TV2, we allow re-bumping the epoch on retry, since we don't complete the epoch bump.
    // Therefore, we don't set hasFailedEpochFence = true.
    warn(s"...with error $error")
  }
```

**How to spot**: When a flag that gates future operations is set inside an error handler, verify the flag's semantics still hold under every protocol version. Here, `hasFailedEpochFence` was designed for TV1 behavior where the epoch *is* committed before the write failure; TV2 never commits the bump, so the flag has the opposite effect.

**Test**: `TransactionCoordinatorTest.testTV2AllowsEpochReBumpingAfterFailedWrite` simulates a three-step flow: (1) first `InitProducerId` fails with `COORDINATOR_NOT_AVAILABLE`, (2) retry triggers an abort and returns `CONCURRENT_TRANSACTIONS`, (3) second retry succeeds with a fresh epoch bump. The test asserts `hasFailedEpochFence` remains `false` throughout and that the final epoch is greater than the initial one.
