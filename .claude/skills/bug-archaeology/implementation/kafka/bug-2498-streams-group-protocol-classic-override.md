**Commit**: 4886414aa0 · **Ticket**: KAFKA-17324
**Subsystem**: streams/StreamsConfig
**Category**: Wrong constant / default value

**Root cause**
Kafka Streams is not compatible with the new KIP-848 consumer rebalance protocol. If a user set `group.protocol=consumer` (or if the broker default changed), Streams would silently use the new protocol and malfunction during rebalances. The fix adds `ConsumerConfig.GROUP_PROTOCOL_CONFIG` to `NON_CONFIGURABLE_CONSUMER_DEFAULT_CONFIGS` and forces its value to `"classic"` in `CONSUMER_DEFAULT_OVERRIDES`, ensuring Streams always overrides any user-supplied or broker-default value for this config.

**Before/After**
```java
// Before: GROUP_PROTOCOL_CONFIG not in non-configurable list
private static final String[] NON_CONFIGURABLE_CONSUMER_DEFAULT_CONFIGS =
    new String[] {ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG};

// After
private static final String[] NON_CONFIGURABLE_CONSUMER_DEFAULT_CONFIGS =
    new String[] {ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                  ConsumerConfig.GROUP_PROTOCOL_CONFIG};
// and in CONSUMER_DEFAULT_OVERRIDES:
tempConsumerDefaultOverrides.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "classic");
```

**How to spot**
When a new consumer configuration key changes fundamental protocol behaviour, verify that frameworks built on top of the consumer (e.g., Streams, Connect) explicitly override it to a safe value. Relying on library users to set the correct value is insufficient when the wrong value causes silent protocol-level incompatibility.

**Test**
`StreamsConfigTest` adds `shouldResetToDefaultIfConsumerGroupProtocolIsOverridden` for main, restore, and global consumers.
