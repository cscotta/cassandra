**Commit**: 2e3b4cb957 · **Ticket**: KAFKA-18352
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The Sarama version listed in the KIP only supports v0. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // Version 0 was removed in Apache Kafka 4.0, Version 1 is the new baseline.
  //
  "validVersions": "1-2",
  // Version 0 was removed in Apache Kafka 4.0, Version 1 is the new baseline.
  //
  "validVersions": "1-2",

// After
  "validVersions": "0-2",
  "validVersions": "0-2",
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18352: add back deletegroups v0, it incorrectly tagged as deprecated (#18324).

**Test**
No test added.
