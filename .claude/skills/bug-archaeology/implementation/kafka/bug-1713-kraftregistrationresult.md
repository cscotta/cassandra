**Commit**: 2a073a14d2 · **Ticket**: KAFKA-18414
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
sealed trait KRaftRegistrationResult
case class FailedRegistrationResult() extends KRaftRegistrationResult
case class SuccessfulRegistrationResult(zkControllerEpoch: Int, controllerEpochZkVersion: Int) extends KRaftRegistrationResult


// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18414 remove kraftregistrationresult (#18401).

**Test**
No test added.
