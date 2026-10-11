**Commit**: 05fd36a3b7 · **Ticket**: KAFKA-18174
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
KAFKA-18174: Subsequent write event completions should be a noop (#18083). The commit addresses a correctness issue in the coordinator-common/src subsystem. The fix ensures the code behaves correctly under all expected conditions.

**Before/After**
```java
// Before
        eventPurgatoryTimeSensor.record(purgatoryTimeMs);

// After
            if (future.isDone()) {
                return;
            }

            if (future.isDone()) {
                return;
            }

import static org.apache.kafka.coordinator.common.runtime.KafkaMetricHistogram.MAX_LATENCY_MS;

        eventPurgatoryTimeSensor.record(Math.min(MAX_LATENCY_MS, purgatoryTimeMs));
import static org.apache.kafka.coordinator.common.runtime.KafkaMetricHistogram.MAX_LATENCY_MS;
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18174: subsequent write event completions should be a noop (#18083).

**Test**
Test modified in CoordinatorRuntimeMetricsImplTest.java covering the corrected behaviour.
