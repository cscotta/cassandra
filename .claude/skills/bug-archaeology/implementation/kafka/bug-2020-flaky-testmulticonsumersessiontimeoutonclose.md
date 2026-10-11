**Commit**: 283d56cf56 · **Ticket**: KAFKA-17904
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig
import java.util.Properties
  override protected def brokerPropertyOverrides(properties: Properties): Unit = {
    super.brokerPropertyOverrides(properties)
    properties.setProperty(GroupCoordinatorConfig.CONSUMER_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG, 500.toString)
    properties.setProperty(GroupCoordinatorConfig.CONSUMER_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG, 500.toString)
  }

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17904: flaky testmulticonsumersessiontimeoutonclose (#17789).

**Test**
Test modified in PlaintextConsumerPollTest.scala covering the corrected behaviour.
