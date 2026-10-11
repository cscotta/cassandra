**Commit**: 21d60eabab · **Ticket**: KAFKA-16673
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
In `GroupMetadataManager#toTopicPartitions`, we generate a list of `ConsumerGroupHeartbeatRequestData.TopicPartitions` from the input deserialized subscription. Currently the input subscription is `ConsumerPartitionAssignor.Subscription`, where the topic partitions are stored as (topic-partition) pairs, whereas in `ConsumerGroupHeartbeatRequestData.TopicPartitions`, we need the topic partitions to be stored as (topic-partition list) pairs. `ConsumerProtocolSubscription` is an intermediate data structure in the deserialization where the topic partitions are stored as (topic-partition list) pairs. This pr uses `ConsumerProtocolSubscription` instead as the input subscription to make `toTopicPar...

**Before/After**
```java
// Before
// (no removals)

// After
    public static ConsumerProtocolSubscription deserializeConsumerProtocolSubscription(
        final ByteBuffer buffer,
        short version
    ) {
        version = checkSubscriptionVersion(version);

        try {
            return new ConsumerProtocolSubscription(new ByteBufferAccessor(buffer), version);
        } catch (BufferUnderflowException e) {
            throw new SchemaException("Buffer underflow while parsing consumer protocol's subscription", e);
        }
    }
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16673; simplify `groupmetadatamanager#totopicpartitions` by using `consumerprotocolsubscription` instead of `consu.

**Test**
Test modified in ConsumerProtocolTest.java covering the corrected behaviour.
