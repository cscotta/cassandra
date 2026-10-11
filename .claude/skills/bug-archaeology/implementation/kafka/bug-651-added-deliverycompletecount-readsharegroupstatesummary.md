**Commit**: 5571821240 · **Ticket**: KAFKA-19799
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR is part of

[KIP-1226](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1226%3A+Introducing+Share+Partition+Lag+Persistence+and+Retrieval). In [KAFKA-19797](https://github.com/apache/kafka/pull/20810), a new
field deliveryCompleteCount was being sent by Share Partition to the
persister. This PR introduces deliveryCompleteCount in
ReadShareGroupStateSummary RPC so that it can later be used to calculate
the share partition lag. Reviewers: Abhinav Dixit <adixit@confluent.io>, Sushant Mahajan
 <smahajan@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
  "validVersions": "0",
  "validVersions": "0",
          "about": "The share-partition start offset." }

// After
        int deliveryCompleteCount,
                            .setDeliveryCompleteCount(deliveryCompleteCount)
  // Version 0 is the initial version (KIP-932).
  //
  // Version 1 introduces DeliveryCompleteCount in the response (KIP-1226).
  "validVersions": "0-1",
  // Version 0 is the initial version (KIP-932).
  //
  // Version 1 introduces DeliveryCompleteCount (KIP-1226).
  "validVersions": "0-1",
          "about": "The share-partition start offset." },
        { "name": "DeliveryCompleteCount", "type": "int32", "versions": "1+", "ignorable": "true", "default": "-1",
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19799: added deliverycompletecount to readsharegroupstatesummary (#20820).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
