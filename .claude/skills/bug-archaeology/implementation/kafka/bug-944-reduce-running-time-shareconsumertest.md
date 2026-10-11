**Commit**: dbbc45f7a0 · **Ticket**: KAFKA-19416
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Configure tests in ShareConsumer to run exclusively in Raft-Isolated
mode to reduce execution time. Reviewers: Andrew Schofield <aschofield@confluent.io>, Jhen-Yung Hsu
<jhenyunghsu@gmail.com>, Ken Huang <s7133700@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.test.api.Type;
    types = {Type.KRAFT},
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19416: reduce running time of shareconsumertest (#19992).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
