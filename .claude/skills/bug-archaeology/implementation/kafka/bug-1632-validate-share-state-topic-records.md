**Commit**: e3a56f3162 · **Ticket**: KAFKA-18513
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
    private static final String DEFAULT_STATE_PERSISTER = "org.apache.kafka.server.share.persister.DefaultStatePersister";
    private static final String NO_OP_PERSISTER = "org.apache.kafka.server.share.persister.NoOpShareStatePersister";
        String persisterClassName = NO_OP_PERSISTER;
        if (testInfo.getDisplayName().contains(".persister=")) {
            persisterClassName = testInfo.getDisplayName().split("=")[1];
        }
            .setConfigProp("group.share.persister.class.name", persisterClassName)
    @ParameterizedTest(name = "{displayName}.persister={0}")
    @ValueSource(strings = {NO_OP_PERSISTER, DEFAULT_STATE_PERSISTER})
    public void testPollNoSubscribeFails(String persister) {

// After
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.internals.Topic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
    private List<TopicPartition> sgsTopicPartitions;
        sgsTopicPartitions = IntStream.range(0, 3)
            .mapToObj(part -> new TopicPartition(Topic.SHARE_GROUP_STATE_TOPIC_NAME, part))
            .toList();
    @Test
    public void testPollNoSubscribeFails() {
    @Test
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18513: validate share state topic records produced in tests. (#18521).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
