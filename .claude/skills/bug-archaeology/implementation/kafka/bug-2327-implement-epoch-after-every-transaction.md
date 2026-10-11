**Commit**: ede0c94aaa · **Ticket**: KAFKA-14562
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Implement server side changes for epoch bump but keep EndTxn as an unstable API until the client side changes are implemented. EndTxnResponse will return the producer ID and epoch for the transaction. Introduces new tagged fields to the TransactionLogValue to persist the clientTransactionVersion, previousProducerId, and nextProducerId to the log so that the state can be reloaded. See KIP-890 for more details.

**Before/After**
```java
// Before
            super(ApiKeys.END_TXN);
  "validVersions": "0-4",
  "validVersions": "0-4",
      "about": "The error code, or 0 if there was no error." }
import org.apache.kafka.server.common.RequestLocal
  private type EndTxnCallback = Errors => Unit
                lastProducerId = RecordBatch.NO_PRODUCER_ID,

// After
            this(data, false);
        }

        public Builder(EndTxnRequestData data, boolean enableUnstableLastVersion) {
            super(ApiKeys.END_TXN, enableUnstableLastVersion);
  //
  // Version 5 enables bumping epoch on every transaction (KIP-890 Part 2)
  "latestVersionUnstable": true,
  "validVersions": "0-5",
  //
  // Version 5 enables bumping epoch on every transaction (KIP-890 Part 2), so producer ID and epoch are included in the response.
  "validVersions": "0-5",
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14562 [1/2]: implement epoch bump after every transaction (#16719).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
