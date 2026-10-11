**Commit**: 1d231b35b8 · **Ticket**: KAFKA-14562
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
A tiny fix to a single protocol JSON which was recently updated under KAFKA-14562. It seems to erroneously use strings where bools appear to be the right type. Reviewers: Andrew Schofield <aschofield@confluent.io>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
    { "name": "ProducerId", "type": "int64", "versions": "5+", "entityType": "producerId", "default": "-1", "ignorable": "true",
    { "name": "ProducerEpoch", "type": "int16", "versions": "5+", "default": "-1", "ignorable": "true",

// After
    { "name": "ProducerId", "type": "int64", "versions": "5+", "entityType": "producerId", "default": "-1", "ignorable": true,
    { "name": "ProducerEpoch", "type": "int16", "versions": "5+", "default": "-1", "ignorable": true,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: use bools for ignorable in endtxnresponse.json (#17558).

**Test**
No test added.
