**Commit**: 11ce41fb1d · **Ticket**: KAFKA-18092
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
  @Flaky("KAFKA-18092")
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18092: mark testbumptransactionalepochwithtv2enabled as flaky (#17945).

**Test**
Test modified in TransactionsTest.scala covering the corrected behaviour.
