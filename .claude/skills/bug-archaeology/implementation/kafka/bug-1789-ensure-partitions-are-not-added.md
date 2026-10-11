**Commit**: f7f3cffb48 · **Ticket**: KAFKA-18227
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
We want to bump the epoch if we are upgrading to TV2. Given that we already have code in place for this, I thought we could piggyback on the completing transaction epoch bump logic. For just initializing producers, I moved the check to the end of InitTransaction. Note, we have do do this check after we initialize the producer ID to ensure we have updated ApiVersions correctly.

**Before/After**
```java
// Before
                    return source == UNINITIALIZED || source == ABORTING_TRANSACTION;
        maybeUpdateTransactionV2Enabled();
    // Check all the finalized features from apiVersions to whether the transaction V2 is enabled.
    public synchronized void maybeUpdateTransactionV2Enabled() {

// After
        transactionManager.maybeUpdateTransactionV2Enabled(true);
                    return source == UNINITIALIZED || source == COMMITTING_TRANSACTION || source == ABORTING_TRANSACTION;
        // Maybe update the transaction version here before we enqueue the EndTxn request so there are no races with
        // completion of the EndTxn request. Since this method may update clientSideEpochBumpRequired, we want to update
        // before the check below, but we also want to call it after the EndTxnRequest.Builder so we complete the transaction
        // with the same version as it started.
        maybeUpdateTransactionV2Enabled(false);

        // If we are upgrading to TV2 transactions on the next transaction, also bump the epoch.
    /**
     *  Check all the finalized features from apiVersions to verify whether the transaction V2 is enabled.
     *  Sets clientSideEpochBumpRequired if upgrading to V2 since we need to bump the epoch.
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18227: ensure v2 partitions are not added to last transaction during upgrade (#18176).

**Test**
Test modified in TransactionManagerTest.java covering the corrected behaviour.
