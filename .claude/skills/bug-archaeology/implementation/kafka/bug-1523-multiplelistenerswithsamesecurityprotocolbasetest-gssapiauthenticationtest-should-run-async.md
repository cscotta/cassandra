**Commit**: e89b30d14e · **Ticket**: KAFKA-18528
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kirk True <ktrue@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))

// After
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18528: multiplelistenerswithsamesecurityprotocolbasetest and gssapiauthenticationtest should run for async consume.

**Test**
Test modified in GssapiAuthenticationTest.scala covering the corrected behaviour.
