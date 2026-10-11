**Commit**: 5e33fe783b · **Ticket**: KAFKA-19740
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Implement [KIP-1237](https://cwiki.apache.org/confluence/x/jIqmFw). - Deprecate the `group.coordinator.rebalance.protocols` configuration
for removal in Kafka 5.0. - Log a deprecation warning when users disable any default protocol
(classic, consumer, streams), advising them to remove the configuration
- Guard warnings with `doLog` to avoid duplicate output during startup
- Clean up redundant `group.coordinator.rebalance.protocols` settings in
test files — `"classic,consumer,streams"` is already the default, and
`share` is a no-op (controlled by `share.version` feature)

Test result:

```
# group.coordinator.rebalance.protocols=classic,consumer,streams
WARN The config `group.coordinator.reba...

**Before/After**
```java
// Before
            @ClusterConfigProperty(key = GroupCoordinatorConfig.GROUP_COORDINATOR_REBALANCE_PROTOCOLS_CONFIG, value = "classic, share"),

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19740 deprecate group.coordinator.rebalance.protocols config (kip-1237) (#21522).

**Test**
Test modified in ShareConsumerRackAwareTest.java covering the corrected behaviour.
