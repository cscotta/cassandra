**Commit**: 7ee5a2ac26 · **Ticket**: KAFKA-19446
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
In this patch for KIP-1228, we add a new WriteTxnMarkersRequest builder
constructor  that includes the    transaction versions for each marker. The  transaction version is already stored in   the metadata on the
transaction coordinator. We now extract this version   information and
pass it to the builder via the TxnMarkerEntry, which propagates it to
partition leaders. Since the TransactionVersion field is marked as ignorable, we always
include it when   building the request.

**Before/After**
```java
// Before
                              List<TopicPartition> partitions) {
            return Objects.hash(producerId, producerEpoch, coordinatorEpoch, result, partitions);
                dataMarkers.add(new WritableTxnMarker()
                                    .setProducerId(marker.producerId)
                                    .setProducerEpoch(marker.producerEpoch)
                                    .setCoordinatorEpoch(marker.coordinatorEpoch)
                                    .setTransactionResult(marker.transactionResult().id)
                                    .setTopics(new ArrayList<>(topicMap.values())));

// After
        private final short transactionVersion;
                              List<TopicPartition> partitions,
                              short transactionVersion) {
            this.transactionVersion = transactionVersion;
        public short transactionVersion() {
            return transactionVersion;
        }

                       ", transactionVersion=" + transactionVersion +
                       transactionVersion == that.transactionVersion &&
            return Objects.hash(producerId, producerEpoch, coordinatorEpoch, result, partitions, transactionVersion);
        /**
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19446: add transaction version to request builder [2/3] (#20868).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
