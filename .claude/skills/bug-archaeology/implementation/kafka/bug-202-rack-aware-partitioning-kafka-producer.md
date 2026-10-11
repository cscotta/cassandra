**Commit**: a3f17327de · **Ticket**: KAFKA-19193
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Implements [KIP-1123](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1123%3A+Rack-aware+partitioning+for+Kafka+Producer). This PR adds the support for rack-aware partitioning to
`BuiltInPartitioner`. It comes with two new configs for the producer:
`partitioner.rack.aware` and `client.rack`, which allows enabling the
new behavior. Apart from the added unit tests, the desired behavior was tested by
`kafka-producer-perf-test.sh` with an existing and a non-existing rack
against a 4 node cluster with two racks and 12-partition topic:
```shell
./kafka_2.13-4.1.0-SNAPSHOT/bin/kafka-producer-perf-test.sh \
  --topic test-topic --num-records 100000 --throughput -1 --record-size
1 \
  --produce...

**Before/After**
```java
// Before
                config.getLong(ProducerConfig.PARTITIONER_AVAILABILITY_TIMEOUT_MS_CONFIG)
            "<li>If no partition or key is present, choose the sticky partition that changes when at least " + BATCH_SIZE_CONFIG + " bytes are produced to the partition.</li>" +

// After
                config.getLong(ProducerConfig.PARTITIONER_AVAILABILITY_TIMEOUT_MS_CONFIG),
                config.getBoolean(ProducerConfig.PARTITIONER_RACK_AWARE_CONFIG),
                config.getString(ProducerConfig.CLIENT_RACK_CONFIG)
    /** <code>partitioner.rack.aware</code> */
    public static final String PARTITIONER_RACK_AWARE_CONFIG = "partitioner.rack.aware";
    private static final String PARTITIONER_RACK_AWARE_DOC = "Controls whether the default partitioner is rack-aware. This has no effect when a custom partitioner is used.";

    /**
     * <code>client.rack</code>
     */
    public static final String CLIENT_RACK_CONFIG = CommonClientConfigs.CLIENT_RACK_CONFIG;
    public static final String DEFAULT_CLIENT_RACK = CommonClientConfigs.DEFAULT_CLIENT_RACK;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19193 support rack-aware partitioning for kafka producer (#19850).

**Test**
Test modified in BuiltInPartitionerTest.java covering the corrected behaviour.
