**Commit**: c8f35f4ea3 · **Ticket**: KAFKA-19774
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Addressing feedback from https://github.com/apache/kafka/pull/21273

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    ctx.config.dynamicConfig.updateDefaultConfig(props)
    ctx.config.dynamicConfig.updateDefaultConfig(props)
    ctx.config.dynamicConfig.updateDefaultConfig(props)
    ctx.config.dynamicConfig.updateDefaultConfig(props)
    ctx.config.dynamicConfig.updateDefaultConfig(props)
    ctx.config.dynamicConfig.updateDefaultConfig(props)

// After
    public LogDirDescription(ApiException error, Map<TopicPartition, ReplicaInfo> replicaInfos, long totalBytes, long usableBytes) {
        this(error, replicaInfos, totalBytes, usableBytes, false);
    }

    ctx.config.dynamicConfig.updateBrokerConfig(0, props)
    ctx.config.dynamicConfig.updateBrokerConfig(0, props)
    ctx.config.dynamicConfig.updateBrokerConfig(0, props)
    ctx.config.dynamicConfig.updateBrokerConfig(0, props)
    ctx.config.dynamicConfig.updateBrokerConfig(0, props)
    ctx.config.dynamicConfig.updateBrokerConfig(0, props)
  * Support for cordoning log directories: For further details, please refer to [KIP-1066](https://cwiki.apache.org/confluence/x/Lg_TEg).
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19774 cleanups for kip-1066 (#21523).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
