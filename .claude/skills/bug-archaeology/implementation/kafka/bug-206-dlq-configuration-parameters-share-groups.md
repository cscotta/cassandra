**Commit**: b6d2503710 · **Ticket**: KAFKA-20410
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
## Summary
This PR adds the configuration foundation for Share Groups Dead-Letter
Queue (DLQ) functionality as specified in [KIP-1191: Dead-letter queues
for share

groups](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1191%3A+Dead-letter+queues+for+share+groups). ## Changes

### New Configurations
**Cluster-level configs (GroupCoordinatorConfig.java):**
1. `errors.deadletterqueue.auto.create.topics.enable` (default: `false`)
     - Enables automatic creation of DLQ topics
2. `errors.deadletterqueue.topic.name.prefix` (default: `"dlq."`)
     - Required prefix for DLQ topic names when auto-create is enabled

**Group-level configs (GroupConfig.java):**
1.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.GroupConfig.{CONSUMER_ASSIGNMENT_INTERVAL_MS_CONFIG, CONSUMER_ASSIGNOR_OFFLOAD_ENABLE_CONFIG, CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, CONSUMER_SESSION_TIMEOUT_MS_CONFIG, SHARE_ASSIGNMENT_INTERVAL_MS_CONFIG, SHARE_ASSIGNOR_OFFLOAD_ENABLE_CONFIG, SHARE_AUTO_OFFSET_RESET_CONFIG, SHARE_DELIVERY_COUNT_LIMIT_CONFIG, SHARE_HEARTBEAT_INTERVAL_MS_CONFIG, SHARE_ISOLATION_LEVEL_CONFIG, SHARE_PARTITION_MAX_RECORD_LOCKS_CONFIG, SHARE_RECORD_LOCK_DURATION_MS_CONFIG, SHARE_RENEW_ACKNOWLEDGE_ENABLE_CONFIG, SHARE_SESSION_TIMEOUT_MS_CONFIG, STREAMS_ASSIGNMENT_INTERVAL_MS_CONFIG, STREAMS_ASSIGNOR_OFFLOAD_ENABLE_CONFIG, STREAMS_HEARTBEAT_INTERVAL_MS_CONFIG, STREAMS_INITIAL_REBALANCE_DELAY_MS_CONFIG, STREAMS_NUM_STANDBY_REPLICAS_CONFIG, STREAMS_SESSION_TIMEOUT_MS_CONFIG, STREAMS_TASK_OFFSET_INTERVAL_MS_CONFIG}

// After

    // Dead Letter Queue Configuration (KIP-1191)
    public static final String ERRORS_DEADLETTERQUEUE_GROUP_ENABLE_CONFIG = "errors.deadletterqueue.group.enable";
    public static final String ERRORS_DEADLETTERQUEUE_GROUP_ENABLE_DOC = "Enable this topic to be used as a dead-letter queue for share groups. " +
        "When set to <code>true</code>, share groups can write undeliverable records to this topic. When set to <code>false</code> (the default), " +
        "attempts to use this topic as a DLQ will be rejected.";
import org.apache.kafka.coordinator.group.GroupConfig.{CONSUMER_ASSIGNMENT_INTERVAL_MS_CONFIG, CONSUMER_ASSIGNOR_OFFLOAD_ENABLE_CONFIG, CONSUMER_HEARTBEAT_INTERVAL_MS_CONFIG, CONSUMER_SESSION_TIMEOUT_MS_CONFIG, SHARE_ASSIGNMENT_INTERVAL_MS_CONFIG, SHARE_ASSIGNOR_OFFLOAD_ENABLE_CONFIG, SHARE_AUTO_OFFSET_RESET_CONFIG, SHARE_DELIVERY_COUNT_LIMIT_CONFIG, SHARE_HEARTBEAT_INTERVAL_MS_CONFIG, SHARE_ISOLATION_LEVEL_CONFIG, SHARE_PARTITION_MAX_RECORD_LOCKS_CONFIG, SHARE_RECORD_LOCK_DURATION_MS_CONFIG, SHARE_RENEW_ACKNOWLEDGE_ENABLE_CONFIG, SHARE_SESSION_TIMEOUT_MS_CONFIG, STREAMS_ASSIGNMENT_INTERVAL_MS_CONFIG, STREAMS_ASSIGNOR_OFFLOAD_ENABLE_CONFIG, STREAMS_HEARTBEAT_INTERVAL_MS_CONFIG, STREAMS_INITIAL_REBALANCE_DELAY_MS_CONFIG, STREAMS_NUM_STANDBY_REPLICAS_CONFIG, STREAMS_SESSION_TIMEOUT_MS_CONFIG, STREAMS_TASK_OFFSET_INTERVAL_MS_CONFIG, ERRORS_DEADLETTERQUEUE_TOPIC_NAME_CONFIG, ERRORS_DEADLETTERQUEUE_COPY_RECORD_ENABLE_CONFIG}
    cgConfigs.put(ERRORS_DEADLETTERQUEUE_TOPIC_NAME_CONFIG, "")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20410: add dlq configuration parameters for share groups (kip-1191) (#21979).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
