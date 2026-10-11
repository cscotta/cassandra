**Commit**: 61299dd01a · **Ticket**: KAFKA-20330
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
A share consumer uses a share session to keep track of its acquired
records with each share-partition leader it is talking to. When the
connection breaks, the share session is lost and acknowledgements fail. When the connection to a share-partition leader failed while there was
an outstanding request, the share consumer noticed the disconnection and
failed the acknowledgements as expected. Also, if the leader changed to
a different broker, again the share consumer noticed the leadership
change and failed the acknowledgements as expected.

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.errors.NotLeaderOrFollowerException;
import org.apache.kafka.common.errors.ShareSessionNotFoundException;
    @ClusterTest
    public void testLeaderRestartWithoutLeadershipChangeExplicitAcknowledgementSync() {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1",
                 Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))) {

            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            shareConsumer.setAcknowledgementCommitCallback((offsetsByTopicPartition, exception) -> {
                assertInstanceOf(NotLeaderOrFollowerException.class, exception);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20330: ack handling improvement on broker restart (#21824).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
