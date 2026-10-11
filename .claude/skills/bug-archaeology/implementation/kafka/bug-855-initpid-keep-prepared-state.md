**Commit**: c7e4ff01cd · **Ticket**: KAFKA-19272

**Subsystem**: Transactions / TransactionManager (producer client)

**Category**: Missing error handler / unimplemented code path

**Root cause**
When a producer receives an `InitProducerId` response with `keepPrepared=true`, it means the broker wants the producer to resume an ongoing transaction using the returned producer ID. The client's `TransactionManager` had a TODO comment but no actual implementation for this case: it did not transition to `PREPARED_TRANSACTION` state when `ongoingTxnProducerId != NO_PRODUCER_ID`. Without this transition the producer would incorrectly treat the epoch bump as a fresh start, silently dropping in-progress transactional state.

**Before/After**
```java
// Before: missing branch, only a TODO
// TODO: handle keepPrepared=true case

// After: explicit state transition
if (keepPrepared && ongoingTxnProducerId != NO_PRODUCER_ID) {
    transitionTo(State.PREPARED_TRANSACTION);
}
```

**How to spot**
Search for `TODO` comments near `InitProducerId` or `keepPrepared` handling in `TransactionManager`. Any path that returns from an `InitPid` response handler without updating the client state machine for all possible response flags is a latent bug.

**Test**
`TransactionManagerTest` adds a scenario where `InitProducerId` returns `keepPrepared=true` with a live `ongoingTxnProducerId` and asserts that the manager enters `PREPARED_TRANSACTION` state.
