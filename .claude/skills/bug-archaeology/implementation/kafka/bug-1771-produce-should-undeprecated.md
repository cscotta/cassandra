**Commit**: 288d4de834 · **Ticket**: KAFKA-18334
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Librdkafka totally breaks if produce v3 is removed - it starts sending records with record format v0. These api versions have to be undeprecated - KIP-896 has been updated. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
  "deprecatedVersions": "0-6",
    requestVersion = 3

// After
  "deprecatedVersions": "0-2",
    requestVersion = 2
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18334: produce v4-v6 should be undeprecated (#18288).

**Test**
Test modified in SocketServerTest.scala covering the corrected behaviour.
