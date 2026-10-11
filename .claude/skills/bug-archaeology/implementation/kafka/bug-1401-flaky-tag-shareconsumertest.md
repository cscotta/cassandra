**Commit**: 7da1a6cbff · **Ticket**: KAFKA-18033
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
3 tests which were marked flaky in ShareConsumerTest do not have any
failure on trunk since the test was converted to use `ClusterTestExtensions`. Reviewers: Sushant Mahajan <smahajan@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky;
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18665")

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18033: remove flaky tag in shareconsumertest (#18995).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
