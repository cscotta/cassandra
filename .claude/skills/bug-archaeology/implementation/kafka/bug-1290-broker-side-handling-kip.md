**Commit**: 3a3159b01e · **Ticket**: KAFKA-18953
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This patch adds logic to enable and handle two phase commit (2PC)
transactions following KIP-939. The changes made are as follows:
1) Add a new broker config called
**transaction.two.phase.commit.enable** which is set to false by default
2) Add new flags **enableTwoPCFlag** and **keepPreparedTxn** to
handleInitProducerId
3) Return an error if keepPreparedTxn is set to true (for now)

Reviewers: Artem Livshits <alivshits@confluent.io>, Justine Olshan
<jolshan@confluent.io>

**Before/After**
```java
// Before
    } else if (!txnManager.validateTransactionTimeoutMs(transactionTimeoutMs)) {
              txnTimeoutMs = transactionTimeoutMs,
            prepareInitProducerIdTransit(transactionalId, transactionTimeoutMs, coordinatorEpoch, txnMetadata,

// After
    public boolean enable2Pc() {
        return data.enable2Pc();
    }

    public boolean keepPreparedTxn() {
        return data.keepPreparedTxn();
    }
      config.transactionStateManagerConfig.transaction2PCEnabled,
                           enableTwoPCFlag: Boolean,
                           keepPreparedTxn: Boolean,
    } else if (enableTwoPCFlag && !txnManager.isTransaction2pcEnabled()) {
      // if the request is to enable two-phase commit but the broker 2PC config is set to false,
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18953: [1/n] add broker side handling for 2 pc (kip-939) (#19193).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
