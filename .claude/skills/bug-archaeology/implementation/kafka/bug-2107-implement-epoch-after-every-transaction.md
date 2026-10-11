**Commit**: e14a81b4b2 · **Ticket**: KAFKA-14562
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This patch includes changes to the clients end transaction response handling when transaction version 2 is enabled. Version 5+ of the End Txn Response includes the producer Id and the producer epoch fields. Upon receiving the request, the client updates its producer Id and epoch according to the response. On receiving an EndTxnRequest the server would've either:

Bumped the epoch for the given producer ID.

**Before/After**
```java
// Before
                            .setCommitted(transactionResult.id));

        public Builder(EndTxnRequestData data) {
            this(data, false);
        public Builder(EndTxnRequestData data, boolean enableUnstableLastVersion) {
  "latestVersionUnstable": true,

// After
                            .setCommitted(transactionResult.id),
                    isTransactionV2Enabled
            );
                // For End Txn version 5+, the broker includes the producerId and producerEpoch in the EndTxnResponse.
                // For versions lower than 5, the producer Id and epoch are set to -1 by default.
                // When Transaction Version 2 is enabled, the end txn request 5+ is used,
                // it mandates bumping the epoch after every transaction.
                // If the epoch overflows, a new producerId is returned with epoch set to 0.
                if (endTxnResponse.data().producerId() != -1) {
                    ProducerIdAndEpoch producerIdAndEpoch = new ProducerIdAndEpoch(
                        endTxnResponse.data().producerId(),
                        endTxnResponse.data().producerEpoch()
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14562 [2/3]: implement epoch bump after every transaction (kip-890) (#17402).

**Test**
Test modified in TransactionManagerTest.java covering the corrected behaviour.
