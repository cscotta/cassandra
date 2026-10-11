**Commit**: 8eb8301294 · **Ticket**: KAFKA-17919
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky
  @Flaky("KAFKA-17463")

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17919 enable back the failing testsharegroups test (#17658).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
