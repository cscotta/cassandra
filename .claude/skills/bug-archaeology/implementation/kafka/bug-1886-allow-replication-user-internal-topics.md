**Commit**: 0bbed823e8 · **Ticket**: KAFKA-17200
**Subsystem**: connect/mirror-client
**Category**: Logic error in condition

**Root cause**
Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>

**Before/After**
```java
// Before
        return  topic.endsWith(internalSuffix());
        return  topic.endsWith(".internal");
        boolean isDefaultConnectTopic =  topic.endsWith("-internal") ||  topic.endsWith(".internal");
        return isMM2InternalTopic(topic) || isKafkaInternalTopic || isDefaultConnectTopic;
        // ends with '.internal': default DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG in standalone mode.
        // ends with '-internal'
        assertTrue(DEFAULT_REPLICATION_POLICY.isInternalTopic("mm2-offsets-CLUSTER-internal"));
    public static final String TOPICS_EXCLUDE_DEFAULT = ".*[\\-\\.]internal, .*\\.replica, __.*";

// After
        return  topic.startsWith("mm2") && topic.endsWith(internalSuffix()) || isCheckpointsTopic(topic);
        return  topic.startsWith("mm2") && topic.endsWith(".internal") || isCheckpointsTopic(topic);
        return isMM2InternalTopic(topic) || isKafkaInternalTopic;
        Map<String, Object> config =  new HashMap<>();
        config.put(MirrorClientConfig.REPLICATION_POLICY_SEPARATOR, ".");
        DEFAULT_REPLICATION_POLICY.configure(config);

        // starts with 'mm2' and ends with '.internal': default DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG in standalone mode.
    public static final String TOPICS_EXCLUDE_DEFAULT = "mm2.*\\.internal, .*\\.replica, __.*";
                <li>
                    The criteria for identifying internal topics in ReplicationPolicy and DefaultReplicationPolicy have
                    been updated to enable the replication of topics that appear to be internal but aren't truly internal to Kafka and Mirror Maker 2.
```

**How to spot**
When reviewing connect/mirror-client code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17200: allow the replication of user internal topics (#17815).

**Test**
Test modified in ReplicationPolicyTest.java covering the corrected behaviour.
