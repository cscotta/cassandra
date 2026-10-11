**Commit**: 6fc6e87382 · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch extends `GroupCoordinatorRecordSerde` to support the `ConsumerGroupRegularExpression` record. Reviewers: Jeff Kim <jeff.kim@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.coordinator.group.generated.ConsumerGroupRegularExpressionKey;
import org.apache.kafka.coordinator.group.generated.ConsumerGroupRegularExpressionValue;
            case 16:
                return new ConsumerGroupRegularExpressionKey();
            case 16:
                return new ConsumerGroupRegularExpressionValue();
import org.apache.kafka.coordinator.group.generated.ConsumerGroupRegularExpressionKey;
import org.apache.kafka.coordinator.group.generated.ConsumerGroupRegularExpressionValue;
        roundTrip((short) 16, new ConsumerGroupRegularExpressionKey(), new ConsumerGroupRegularExpressionValue());
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [6/n] add new record to groupcoordinatorrecordserde (#17791).

**Test**
Test modified in GroupCoordinatorRecordSerdeTest.java covering the corrected behaviour.
