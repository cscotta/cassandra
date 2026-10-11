**Commit**: fdeabe552f · **Ticket**: KAFKA-20229
**Subsystem**: connect/mirror-client
**Category**: Race condition / missing synchronization

**Root cause**
Implements [KIP-1239](https://cwiki.apache.org/confluence/x/h4HMFw)

Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>

**Before/After**
```java
// Before
     * Translates a remote consumer group's offsets into corresponding local offsets. Topics are automatically
     * @param consumerGroupId The group ID of remote consumer group
    public Map<TopicPartition, OffsetAndMetadata> remoteConsumerOffsets(String consumerGroupId,
            String remoteClusterAlias, Duration timeout) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(consumerConfig,
                new ByteArrayDeserializer(), new ByteArrayDeserializer())) {
            List<TopicPartition> checkpointAssignment =
                List.of(new TopicPartition(checkpointTopic, 0));
                        if (checkpoint.consumerGroupId().equals(consumerGroupId)) {
                            offsets.put(checkpoint.topicPartition(), checkpoint.offsetAndMetadata());
            log.info("Consumed {} checkpoint records for {} from {}.", offsets.size(),

// After
import java.util.regex.Pattern;
    // for testing
    Consumer<byte[], byte[]> consumer() {
        return new KafkaConsumer<>(consumerConfig, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    }

     * Translates remote consumer groups' offsets into corresponding local offsets. Topics are automatically
     * @param consumerGroupPattern The regex pattern specifying the consumer groups to translate offsets for
     * @throws IllegalArgumentException If any of the arguments are null
    public Map<String, Map<TopicPartition, OffsetAndMetadata>> remoteConsumerOffsets(Pattern consumerGroupPattern,
             String remoteClusterAlias, Duration timeout) {
        if (consumerGroupPattern == null) {
```

**How to spot**
When reviewing connect/mirror-client code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20229: batch offset translation in remoteclusterutils (#21591).

**Test**
Test modified in MirrorClientTest.java covering the corrected behaviour.
