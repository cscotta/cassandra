**Commit**: ede01b871e · **Ticket**: KAFKA-20246
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Implementation of

https://cwiki.apache.org/confluence/display/KAFKA/KIP-1242%3A+Detection+and+handling+of+misrouted+connections.

**Before/After**
```java
// Before
// (no removals)

// After
    public static final String METADATA_CLUSTER_CHECK_ENABLE_CONFIG = "metadata.cluster.check.enable";
    public static final String METADATA_CLUSTER_CHECK_ENABLE_DOC = "Whether the client should send cluster and node information " +
            "when connecting to a broker to enable it to check for a misrouted connection. This configuration is ignored if " +
            "rebootstrapping is disabled by setting the configuration <code>metadata.recovery.strategy=none</code>. If the client " +
            "is connecting to a broker older than Apache Kafka 4.4, no checking is performed and this configuration has no effect.";

    public static final String METADATA_CLUSTER_CHECK_ENABLE_CONFIG = CommonClientConfigs.METADATA_CLUSTER_CHECK_ENABLE_CONFIG;
    public static final String METADATA_CLUSTER_CHECK_ENABLE_DOC = CommonClientConfigs.METADATA_CLUSTER_CHECK_ENABLE_DOC;

                                .define(METADATA_CLUSTER_CHECK_ENABLE_CONFIG,
                                        Type.BOOLEAN,
                                        true,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20246: detection and handling of misrouted connections [1/n] (#21766).

**Test**
Test modified in NetworkClientTest.java covering the corrected behaviour.
