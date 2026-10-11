**Commit**: 8f2ee4d7cd · **Ticket**: KAFKA-18117
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR uses topic IDs received in assignment (under new protocol) to
ensure that only these assigned topics are included in the consumer
metadata requests performed when the user subscribes to broker-side
regex (RE2J). For handling the edge case of consumer needing metadata for topics IDs
(from RE2J) and topic names (from transient topics), the approach is to
send a request for the transient topics needed temporarily, and once
those resolved, the request for the topic IDs needed for RE2J will
follow. (this is because the broker doesn't accept requests for names
and IDs at the same time)

With the changes we also end up fixing another issue (KAFKA-18729) aimed
at avoiding iterating the full ...

**Before/After**
```java
// Before
            if (!retainTopic(topicName, metadata.isInternal(), nowMs))

// After
    @ClusterTest
    public void testTopicIdSubscriptionWithRe2JRegexAndOffsetsFetch() throws InterruptedException {
        var topic1 = "topic1"; // matches subscribed pattern
        cluster.createTopic(topic1, 2, (short) BROKER_COUNT);

        Map<String, Object> config = Map.of(GROUP_PROTOCOL_CONFIG, GroupProtocol.CONSUMER.name().toLowerCase(Locale.ROOT));
        try (
                Producer<byte[], byte[]> producer = cluster.producer();
                Consumer<byte[], byte[]> consumer = cluster.consumer(config)
        ) {
            assertEquals(0, consumer.assignment().size());

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18117; kafka-18729: use assigned topic ids to avoid full metadata requests on broker-side regex (#19814).

**Test**
Test modified in PlaintextConsumerSubscriptionTest.java covering the corrected behaviour.
