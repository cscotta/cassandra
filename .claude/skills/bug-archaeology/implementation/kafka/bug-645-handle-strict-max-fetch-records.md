**Commit**: c205e6910f · **Ticket**: KAFKA-19020
**Subsystem**: checkstyle/import-control.xml
**Category**: Resource leak

**Root cause**
JIRA:

[https://issues.apache.org/jira/projects/KAFKA/issues/KAFKA-19020](https://issues.apache.org/jira/projects/KAFKA/issues/KAFKA-19020)
KIP :

[here](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1206:+Strict+max+fetch+records+in+share+fetch)
The `maxFetchRecords` parameter in shared fetch operations currently
acts as a soft limit, meaning the actual number of records returned to
the client may exceed this threshold under certain conditions. This PR
introduces an `AcquireMode` in the `ShareFetchRequest` to offer clients
a more flexible method for acquiring records. By setting this value to
strict, consumers can ensure that the actual number of records received
will never exceed t...

**Before/After**
```java
// Before
// (no removals)

// After
      <allow pkg="org.apache.kafka.clients.consumer" />
    private static final String RECORD_LIMIT = "record_limit";
    private static final String BATCH_OPTIMIZED = "batch_optimized";
    @ClusterTest
    public void testPollInBatchOptimizedMode() {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
                "group1",
                Map.of(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5, ConsumerConfig.SHARE_ACQUIRE_MODE_CONFIG, BATCH_OPTIMIZED))
        ) {
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(tp.topic(), tp.partition(), null, "key".getBytes(), "value".getBytes());
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19020 [1/n]: handle strict max fetch records in share fetch (#20246).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
