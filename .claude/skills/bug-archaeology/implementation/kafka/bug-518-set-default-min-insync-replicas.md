**Commit**: f1a664f5d6 · **Ticket**: KAFKA-19858
**Subsystem**: docs/getting-started
**Category**: Logic error in condition

**Root cause**
[KIP-1235](https://cwiki.apache.org/confluence/x/yommFw)

The __remote_log_metadata internal topic currently lacks a configurable
min.insync.replicas setting, relying on the broker-level default
(typically 1) when the factor is 3 as default. This creates a data loss
risk in production environments, as writes may be acknowledged by only a
single replica. What's more, this is inconsistent with some other cases such as
__transaction_state, which explicitly sets min.insync.replicas=2 via the
transaction.state.log.min.isr broker configuration. Both topics store
critical metadata and should have equivalent durability guarantees.

**Before/After**
```java
// Before

    # This needs to be changed if number of brokers in the cluster is more than 1

// After
  * The new config have been introduced: `remote.log.metadata.topic.min.isr` with 2 as default value. You can correct the min.insync.replicas for the existed __remote_log_metadata topic via kafka-configs.sh if needed. For further details, please refer to [KIP-1235](https://cwiki.apache.org/confluence/x/yommFw).
    # For single broker cluster, set this to 1. Default is 3 for clusters with 3 or more brokers.
    # The minimum number of replicas that must acknowledge a write to remote log metadata topic.
    # Default value is 2. For single broker cluster (replication factor = 1), set this to 1.
    rlmm.config.remote.log.metadata.topic.min.isr=1

        topicConfigs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, Short.toString(rlmmConfig.metadataTopicMinIsr()));
    public static final String REMOTE_LOG_METADATA_TOPIC_MIN_ISR_PROP = "remote.log.metadata.topic.min.isr";
```

**How to spot**
When reviewing docs/getting-started code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19858 set default min.insync.replicas=2 for __remote_log_metadata topic to prevent data loss (kip-1235) (#20811).

**Test**
Test modified in RemoteLogMetadataManagerTestUtils.java covering the corrected behaviour.
