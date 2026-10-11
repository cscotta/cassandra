**Commit**: 9e620287f1 · **Ticket**: KAFKA-19118
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Enable KIP-1071 parameter in `StandbyTaskCreationIntegrationTest`. Required a fix: In `ChangelogTopic.setup`, we actually need to return
both the source-topic (optimized) and the non-source-topic changelog
topics, since otherwise we will not find the partition number later on. Extended `EmbeddedKafkaCluster` to set the number of standby replicas
dynamically for the group. We need to initialize it to one for the
integration test to go through.

**Before/After**
```java
// Before
import java.util.HashSet;
import java.util.Set;
     * Determines the number of partitions for each non-source changelog topic in the requested topology.
     * @return the map of all non-source changelog topics for the requested topology to their required number of partitions.
            final Set<String> sourceTopics = new HashSet<>(subtopology.sourceTopics());

                if (!sourceTopics.contains(topicInfo.name())) {
                    changelogTopicPartitions.put(topicInfo.name(), maxNumPartitions.getAsInt());
                }
    public void shouldNotContainSourceBasedChangelogs() {
        assertEquals(Map.of(), setup);
        assertEquals(Map.of(CHANGELOG_TOPIC_CONFIG.name(), 3), setup);

// After
     * Determines the number of partitions for each changelog topic in the requested topology.
     * @return the map of all changelog topics for the requested topology to their required number of partitions.
                changelogTopicPartitions.put(topicInfo.name(), maxNumPartitions.getAsInt());
    public void shouldContainSourceBasedChangelogs() {
        assertEquals(Map.of(SOURCE_TOPIC_NAME, 3), setup);
        assertEquals(Map.of(CHANGELOG_TOPIC_CONFIG.name(), 3, SOURCE_TOPIC_NAME, 3), setup);
import org.apache.kafka.streams.GroupProtocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19118: enable kip-1071 in standbytaskcreationintegrationtest (#19438).

**Test**
Test modified in ChangelogTopicsTest.java covering the corrected behaviour.
