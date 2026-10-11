**Commit**: 753a003480 · **Ticket**: KAFKA-18262
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * Check if partitioner is deprecated and log a warning if it is.
     */
    @SuppressWarnings("deprecation")
    private void warnIfPartitionerDeprecated() {
        // Using DefaultPartitioner and UniformStickyPartitioner is deprecated, see KIP-794.
        if (partitioner instanceof org.apache.kafka.clients.producer.internals.DefaultPartitioner) {
            log.warn("DefaultPartitioner is deprecated.  Please clear " + ProducerConfig.PARTITIONER_CLASS_CONFIG
                    + " configuration setting to get the default partitioning behavior");
        }
        if (partitioner instanceof org.apache.kafka.clients.producer.UniformStickyPartitioner) {
            log.warn("UniformStickyPartitioner is deprecated.  Please clear " + ProducerConfig.PARTITIONER_CLASS_CONFIG

// After
        if (partitioner == null) {
            return this.cluster.partitionsForTopic(record.topic()).get(0).partition();
        }
     * Note this method is only implemented in DefaultPartitioner and UniformStickyPartitioner which
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18262 remove defaultpartitioner and uniformstickypartitioner (#18204).

**Test**
Test modified in MockProducerTest.java covering the corrected behaviour.
