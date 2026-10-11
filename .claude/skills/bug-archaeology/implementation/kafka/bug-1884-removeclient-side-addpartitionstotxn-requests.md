**Commit**: 755adf8a56 · **Ticket**: KAFKA-14563
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Removes the client side AddPartitionsToTxn/AddOffsetsToTxn calls so that the partition is implicitly added as part of KIP-890 part 2. This change also requires updating the valid state transitions. The client side can not know for certain if a partition has been added server side when the request times out (partial completion). Thus for TV2, the transition to PrepareAbort is now valid for Empty, CompleteCommit, and CompleteAbort.

**Before/After**
```java
// Before
                keySerializer, valueSerializer, null, null, null, Time.SYSTEM);
            this.apiVersions = new ApiVersions();
                        .setTopicData(tpd));
        log.debug("Begin adding offsets {} for consumer group {} to transaction", offsets, g

// After
                keySerializer, valueSerializer, null, null, null, new ApiVersions(), Time.SYSTEM);
                  ApiVersions apiVersions,
            this.apiVersions = apiVersions;
    // Visible for testing
    TransactionManager getTransactionManager() {
        return transactionManager;
    }


        // When we use transaction V1 protocol in transaction we set the request version upper limit to
        // LAST_STABLE_VERSION_BEFORE_TRANSACTION_V2 so that the broker knows that we're using transaction protocol V1.
        boolean useTransactionV1Version = false;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14563: removeclient-side addpartitionstotxn requests (#17698).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
