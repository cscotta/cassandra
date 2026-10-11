**Commit**: c977bfdd3c · **Ticket**: KAFKA-17413
**Subsystem**: core/GroupCoordinator / KafkaConfig
**Category**: Wrong constant / default value

**Root cause**
The `group.version` feature flag was dropped in an earlier refactor and the `isConsumerGroupProtocolEnabled()` predicate had no version gate, meaning the new consumer group protocol could be enabled on brokers that hadn't negotiated IBP_4_0_IV2. The fix re-introduces the `group.version` feature level check: `isConsumerGroupProtocolEnabled()` now returns true only when `groupVersion().isConsumerRebalanceProtocolSupported`, and the default metadata version used in tests was bumped from IBP_4_0_IV1 to IBP_4_0_IV2 to reflect the new minimum required level.

**Before/After**
```java
// Before
def isConsumerGroupProtocolEnabled(): Boolean = consumerGroupEnabled

// After
def isConsumerGroupProtocolEnabled(): Boolean =
    groupVersion().isConsumerRebalanceProtocolSupported
```

**How to spot**
When a feature is gated by a metadata/IBP version, the predicate that checks whether it is enabled must consult the version level, not just a boolean flag. If the flag is removed or the version check is dropped, the feature can activate on clusters that have not yet completed a rolling upgrade.

**Test**
Existing group coordinator tests updated to pass IBP_4_0_IV2 as the default metadata version.
