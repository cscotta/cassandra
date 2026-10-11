**Commit**: 81d86e59c2 · **Ticket**: KAFKA-19845
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
* A couple of additional share consumer tests to check dual ack with
renew and poll behavior with renew acks piggybacked. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.server.metrics.KafkaYammerMetrics;
import com.yammer.metrics.core.Meter;

import org.junit.jupiter.api.AfterEach;
    @AfterEach
    public void tearDown() {
        kafka.utils.TestUtils.clearYammerMetrics();
    }

        verifyYammerMetricCount("ackType=Renew", 5);
        verifyYammerMetricCount("ackType=Renew", 5);
    }
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19845: 2/n additional share consumers tests. (#20900).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
