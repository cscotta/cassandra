**Commit**: 84502429bb · **Ticket**: KAFKA-19779
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
For validating offset commits based on partitions, we need to be able to
efficiently find the subtopology from a given source topic (user input
topic or repartition topic). We introduce this precomputed map in `StreamsTopology`, where it is
generated upon construction. We can use the precomputed map in StreamsGroup.isSuscribedToTopic we can
use it to validate efficiently if we are still subscribed to a certain
topic. Using `StreamTopology` here instead of `ConfiguredTopology` is an
improvement in itself, because configured topology is soft state and not
entirely reliable to always be there.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.streams.topics.ConfiguredSubtopology;
import java.util.TreeMap;
        Optional<ConfiguredTopology> maybeConfiguredTopology = configuredTopology.get();
        if (maybeConfiguredTopology.isEmpty() || !maybeConfiguredTopology.get().isReady()) {
        for (ConfiguredSubtopology sub : maybeConfiguredTopology.get().subtopologies().orElse(new TreeMap<>()).values()) {
            if (sub.sourceTopics().contains(topic) || sub.repartitionSourceTopics().containsKey(topic)) {
                return true;
            }
        }
        return false;
                              Map<String, Subtopology> subtopologies) {

// After
        Optional<StreamsTopology> maybeTopology = topology.get();
        if (maybeTopology.isEmpty()) {
        return maybeTopology.get().sourceTopicMap().containsKey(topic);
import java.util.HashMap;
 * @param sourceTopicMap A precomputed map of source topics to their corresponding subtopology (must be non-null)
                              Map<String, Subtopology> subtopologies,
                              Map<String, Subtopology> sourceTopicMap) {

    /**
     * Constructor that automatically computes the sourceTopicMap from subtopologies.
     *
     * @param topologyEpoch The epoch of the topology (must be non-negative).
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19779: introduce source topic to subtopology map [2/n] (#20717).

**Test**
Test modified in StreamsGroupTest.java covering the corrected behaviour.
