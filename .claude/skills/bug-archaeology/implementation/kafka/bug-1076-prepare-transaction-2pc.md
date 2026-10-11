## Commit

7f02c263a6 — KAFKA-19082 [3/4]: Add prepareTransaction to KafkaProducer for KIP-939 two-phase commit

## Ticket

KAFKA-19082

## Subsystem

Clients / transaction manager

## Category

Missing null / bounds check

## Root cause

KIP-939 (two-phase commit for Kafka transactions) requires a `prepareTransaction()` step between `beginTransaction()` and `commitTransaction()`. Without this step, external coordinators cannot atomically mark a transaction as "prepared" before deciding to commit or abort. The `TransactionManager` state machine lacked a `PREPARED_TRANSACTION` state, so any code that called `sendOffsetsToTransaction` or `commitTransaction` after preparing would proceed without error, silently bypassing the 2PC protocol. The fix adds the `PREPARED_TRANSACTION` state and guards that throw `IllegalStateException` if the producer attempts operations that are not valid in the prepared state.

## Before / After

```java
// Before — no PREPARED_TRANSACTION state, 2PC not enforceable
public synchronized void beginTransaction() { ... }
public synchronized void commitTransaction() { ... }

// After — PREPARED_TRANSACTION state with illegal-operation guards
public void prepareTransaction() {
    transactionManager.transitionTo(PREPARED_TRANSACTION);
}
@Override
public void commitTransaction() {
    if (transactionManager.isInState(PREPARED_TRANSACTION))
        throw new IllegalStateException("Cannot commit from prepared state");
    ...
}
```

## How to spot

Search for `TransactionManager` state transition tables. Any 2PC protocol implementation that lacks a distinct "prepared" state between "open" and "committed" cannot enforce the atomic decision step, allowing coordinator failures to leave transactions in an ambiguous state.

## Test

`TransactionManagerTest` was extended with cases that verify `prepareTransaction()` transitions to `PREPARED_TRANSACTION` and that subsequent `commitTransaction()` and `sendOffsetsToTransaction` calls throw `IllegalStateException`.
