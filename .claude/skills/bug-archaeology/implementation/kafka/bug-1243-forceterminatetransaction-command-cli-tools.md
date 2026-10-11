**Commit**: eeffd8c475 · **Ticket**: KAFKA-19003
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch is part of KIP-939 [Support Participation in
2PC](https://cwiki.apache.org/confluence/display/KAFKA/KIP-939%3A+Support+Participation+in+2PC)

The kafka-transactions.sh tool will support a new command
--forceTerminateTransaction It has one required argument
--transactionalId that would take the transactional id for the
transaction to be terminated. The command uses the existing Admin#fenceProducers method to forcefully
abort the transaction associated with the specified transactional ID. Under the hood, it sends an InitProducerId request to the transaction
coordinator with the given transactional ID and keepPreparedTxn = false
by default. This is aligned with the functionality outl...

**Before/After**
```java
// Before
// (no removals)

// After

    /**
     * Force terminate a transaction for the given transactional ID with the default options.
     * <p>
     * This is a convenience method for {@link #forceTerminateTransaction(String, TerminateTransactionOptions)}
     * with default options.
     *
     * @param transactionalId           The ID of the transaction to terminate.
     * @return The TerminateTransactionResult.
     */
    default TerminateTransactionResult forceTerminateTransaction(String transactionalId) {
        return forceTerminateTransaction(transactionalId, new TerminateTransactionOptions());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19003: add forceterminatetransaction command to cli tools (#19276).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
