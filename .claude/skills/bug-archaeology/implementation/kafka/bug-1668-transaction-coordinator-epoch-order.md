## Commit

- Hash: 32dbbe6a1f
- Tickets: KAFKA-18464
- Subject: KAFKA-18464: Fix producer self-fencing on empty abort after commit in Transaction V2

## Subsystem

core / TransactionCoordinator

## Category

Logic error in condition

## Root cause

In Transaction V2 (TV2), when a producer sent an `EndTransaction(ABORT)` after its commit had already been persisted (but before the transition completed), the `handleEndTransaction` logic checked `!isValidEpoch` before `pendingTransitionInProgress`. The epoch check fired first because the coordinator was mid-transition and returned `PRODUCER_FENCED`, causing the producer to fence itself unnecessarily. Swapping the guard order ensures the in-flight transition is detected first and `CONCURRENT_TRANSACTIONS` is returned instead, letting the producer retry.

## Before

```scala
if (!isValidEpoch(producerEpoch, txnMetadata)) {
    return Left(Errors.PRODUCER_FENCED)
}
if (txnMetadata.pendingTransitionInProgress) {
    return Left(Errors.CONCURRENT_TRANSACTIONS)
}
```

## After

```scala
if (txnMetadata.pendingTransitionInProgress) {
    return Left(Errors.CONCURRENT_TRANSACTIONS)
}
if (!isValidEpoch(producerEpoch, txnMetadata)) {
    return Left(Errors.PRODUCER_FENCED)
}
```

## How to spot

Multi-guard condition sequences in stateful coordinators where an in-progress transition can cause a stale-epoch appearance — the transition check must always precede the epoch check to avoid false fencing.

## Test

`TransactionCoordinatorTest` extended with the empty-abort-after-commit scenario in TV2 mode, asserting `CONCURRENT_TRANSACTIONS` rather than `PRODUCER_FENCED`.
