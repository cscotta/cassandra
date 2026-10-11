**Commit**: f42abe6db8 · **Ticket**: KAFKA-19082
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
public void completeTransaction(PreparedTxnState preparedTxnState)

The method compares the currently prepared transaction state and the
state passed in the argument. 1. Commit if the state matches
2. Abort the transaction otherwise.

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Completes a prepared transaction by comparing the provided prepared transaction state with the
     * current prepared state on the producer.
     * If they match, the transaction is committed; otherwise, it is aborted.
     *
     * @param preparedTxnState              The prepared transaction state to compare against the current state
     * @throws IllegalStateException if no transactional.id has been configured or no transaction has been started
     * @throws InvalidTxnStateException if the producer is not in prepared state
     * @throws ProducerFencedException fatal error indicating another producer with the same transactional.id is active
     * @throws KafkaException if the producer has encountered a previous fatal error or for any other unexpected error
     * @throws TimeoutException if the time taken for completing the transaction has surpassed <code>max.block.ms</code>
     * @throws InterruptException if the thread is interrupted while blocked
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19082:[4/4] complete txn client side changes (kip-939) (#19714).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
