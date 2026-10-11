**Commit**: 632aedcf4f · **Ticket**: KAFKA-18632
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        if (metadataCache.contains(internalTopicName)) {
            Set<String> topicSet = new HashSet<>();
            topicSet.add(internalTopicName);

            List<MetadataResponseData.MetadataResponseTopic> topicMetadata = CollectionConverters.asJava(
                metadataCache.getTopicMetadata(
                    CollectionConverters.asScala(topicSet),
                    interBrokerListenerName,
                    false,
                    false
                )
            );

// After
import org.apache.kafka.common.errors.CoordinatorNotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    private final Logger log = LoggerFactory.getLogger(ShareCoordinatorMetadataCacheHelperImpl.class);
        try {
            if (metadataCache.contains(internalTopicName)) {
                Set<String> topicSet = new HashSet<>();
                topicSet.add(internalTopicName);
                List<MetadataResponseData.MetadataResponseTopic> topicMetadata = CollectionConverters.asJava(
                    metadataCache.getTopicMetadata(
                        CollectionConverters.asScala(topicSet),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18632: multibroker test improvements. (#18718).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
