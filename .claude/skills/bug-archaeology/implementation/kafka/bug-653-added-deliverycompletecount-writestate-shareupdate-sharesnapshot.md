**Commit**: b900f2fe5a · **Ticket**: KAFKA-19797
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR is part of

[KIP-1226](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1226%3A+Introducing+Share+Partition+Lag+Persistence+and+Retrieval). It introduces a new field called `deliveryCompleteCount` in
writeShareGroupState RPC and `ShareUpdate` and `ShareSnapshot`. The
share partition now maintains `inFlightTerminalCount` (introduced
[here](https://github.com/apache/kafka/pull/20787)) and it uses this
exact value to pass to the writeState request for
`deliveryCompleteCount`. Reviewers: Andrew Schofield <aschofield@confluent.io>, Abhinav Dixit
 <adixit@confluent.io>

**Before/After**
```java
// Before
  "validVersions": "0",
  "validVersions": "0",
     * The inFlightTerminalRecords tracks the number of terminal (ACKNOWLEDGED / ARCHIVED) records within the
    private final AtomicInteger inFlightTerminalRecords;
        this.inFlightTerminalRecords = new AtomicInteger(0);
                    // During initialization, inFlightTerminalRecords is updated with the number of records that are in the


// After
  // Version 0 is the initial version (KIP-932).
  //
  // Version 1 introduces DeliveryCompleteCount (KIP-1226).
  "validVersions": "0-1",
        { "name": "DeliveryCompleteCount", "type": "int32", "versions": "1+", "ignorable": "true", "default": "-1",
          "about": "The number of offsets greater than or equal to share-partition start offset for which delivery has been completed." },
  // Version 0 is the initial version (KIP-932).
  //
  // Version 1 introduces DeliveryCompleteCount in the request (KIP-1226).
  "validVersions": "0-1",
                    .setDeliveryCompleteCount(0)
     * The deliveryCompleteCount tracks the number of terminal (ACKNOWLEDGED / ARCHIVED) records within the
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19797: added deliverycompletecount to writestate, shareupdate and sharesnapshot schemas (#20810).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
