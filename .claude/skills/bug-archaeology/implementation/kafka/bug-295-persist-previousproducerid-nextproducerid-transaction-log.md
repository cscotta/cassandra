**Commit**: eb111f6695 · **Ticket**: KAFKA-20310
**Subsystem**: transaction-coordinator/src
**Category**: Wrong serialization / deserialization

**Root cause**
## Summary

`TransactionLog.valueToBytes()` does not set `PreviousProducerId` or
`NextProducerId` on the `TransactionLogValue` when serializing
transaction metadata. Both fields exist in the schema (tagged fields at
version 1+, tags 0 and 1) and are correctly read back by
`readTxnRecord()`, but because they are never written, they always
deserialize as `-1` (`NO_PRODUCER_ID`) after coordinator failover. ## Impact

This causes two failure modes described in KAFKA-20310:

1. **Coordinator failover during epoch exhaustion:** `nextProducerId` is
lost, so `prepareComplete()` cannot rotate to the new producer ID.

**Before/After**
```java
// Before
        return MessageUtil.toVersionPrefixedBytes(
                transactionVersionLevel.transactionLogValueVersion(),
                new TransactionLogValue()
                        .setClientTransactionVersion(txnMetadata.clientTransactionVersion().featureLevel())
        );

// After
        short logValueVersion = transactionVersionLevel.transactionLogValueVersion();
        TransactionLogValue value = new TransactionLogValue()
                        .setClientTransactionVersion(txnMetadata.clientTransactionVersion().featureLevel());

        if (logValueVersion >= 1) {
            value.setPreviousProducerId(txnMetadata.prevProducerId());
            value.setNextProducerId(txnMetadata.nextProducerId());
        }

        return MessageUtil.toVersionPrefixedBytes(logValueVersion, value);
    @Test
    void shouldRoundTripPreviousAndNextProducerIds() {
```

**How to spot**
When reviewing transaction-coordinator/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20310: persist previousproducerid and nextproducerid in transaction log (#21828).

**Test**
Test modified in TransactionLogTest.java covering the corrected behaviour.
