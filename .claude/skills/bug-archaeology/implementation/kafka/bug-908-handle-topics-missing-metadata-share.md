**Commit**: 268cf664c3 · **Ticket**: KAFKA-19454
**Subsystem**: group-coordinator/src
**Category**: Missing null / bounds check

**Root cause**
* There are instances where share group delete calls in group
coordinator (`onPartitionsDelete`, `deleteShareGroups`) where we lookup
the metadata image to fetch the topic id/partitions/topic name for a
topic name/id. However, there have been
instances where the looked up info was not found due to cluster being
under load or the underlying topic being deleted and information not
propagated correctly. * To remedy the same, this PR adds checks to determine that topic is
indeed present in the image before the lookups thus preventing NPEs. The
problematic situations are logged.

**Before/After**
```java
// Before
        // with the deleted topics.
        Set<Uuid> topicIds = topicPartitions.stream()
            .map(tp -> metadataImage.topics().getTopic(tp.topic()).id())
            .collect(Collectors.toSet());
        Map<Uuid, InitMapValue> deletingTopics = shareGroupStatePartitionMetadata.get(shareGroupId).deletingTopics().stream()
            .map(tid -> {
                TopicImage image = metadataImage.topics().getTopic(tid);
                return Map.entry(tid, new InitMapValue(image.name(), image.partitions().keySet(), -1));
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!deletingTopics.isEmpty()) {
            log.info("Existing deleting entries found in share group {} - {}", shareGroupId, deletingTopics);

// After
import org.apache.kafka.image.TopicImage;
        // with the deleted topics. However, we must guard against it.
        if (metadataImage == null || metadataImage.equals(MetadataImage.EMPTY)) {
            return;
        }

        Set<Uuid> topicIds = new HashSet<>();
        for (TopicPartition tp : topicPartitions) {
            TopicImage image = metadataImage.topics().getTopic(tp.topic());
            if (image != null) {
                topicIds.add(image.id());
            }
```

**How to spot**
When reviewing group-coordinator/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19454: handle topics missing in metadata in share delete. (#20090).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
