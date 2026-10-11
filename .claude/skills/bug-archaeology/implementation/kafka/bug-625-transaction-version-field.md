**Commit**: faad21fcb9 · **Ticket**: KAFKA-19446
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Simple PR to add the transaction version field to the WriteTxnMarkers
Request in order to enable KIP-1228. We are bumping the API version to
version 2. Reviewers: Artem Livshits <alivshits@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
            super(ApiKeys.WRITE_TXN_MARKERS, (short) 1); // if we add new versions, gate them behind metadata version
  "validVersions": "1",
        "about": "Epoch associated with the transaction state partition hosted by this transaction coordinator." }
  "validVersions": "1",

// After
            // version will be determined at build time based on broker capabilities
            super(ApiKeys.WRITE_TXN_MARKERS);
  // Version 2 adds TransactionVersion field to the WritableTxnMarker (KIP-1228).
  "validVersions": "1-2",
        "about": "Epoch associated with the transaction state partition hosted by this transaction coordinator." },
      { "name": "TransactionVersion", "type": "int8", "versions": "2+", "ignorable": true,
        "about": "Transaction version of the marker. Ex: 0/1 = legacy (TV0/TV1), 2 = TV2 etc.", "default": "0" }
  "validVersions": "1-2",
  // Version 2 matches WriteTxnMarkersRequest version 2 (KIP-1228).
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19446: add transaction version field [1/3] (#20857).

**Test**
No test added.
