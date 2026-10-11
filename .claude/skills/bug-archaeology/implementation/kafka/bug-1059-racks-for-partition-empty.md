## Commit

c493d89334 — KAFKA-17747 [3/N]: racksForPartition always returns empty set in SubscribedTopicDescriberImpl

## Ticket

KAFKA-17747

## Subsystem

Group coordinator / rack-aware partition assignment

## Category

Silent skip on non-match

## Root cause

`SubscribedTopicDescriberImpl` held a `Map<String, TopicMetadata>` keyed by topic name to resolve partition rack information. However, the map was populated from `TopicMetadata` objects that stored racks in an internal structure not keyed the same way as the lookup path in `racksForPartition()`. The lookup always missed and returned an empty set, meaning the rack-aware assignment algorithm received no rack data and fell back to rack-unaware placement silently. The fix replaces `TopicMetadata` with the full `MetadataImage` which has a correctly indexed topic-partition-to-rack mapping.

## Before / After

```java
// Before — map lookup always misses, returns empty set
public Set<String> racksForPartition(Uuid topicId, int partition) {
    TopicMetadata meta = topicMetadataMap.get(topicId);
    if (meta == null) return Collections.emptySet();  // always hit
    return meta.racksForPartition(partition);
}

// After — uses MetadataImage with correct partition-rack index
public Set<String> racksForPartition(Uuid topicId, int partition) {
    TopicImage topic = metadataImage.topics().getTopic(topicId);
    if (topic == null) return Collections.emptySet();
    return topic.partitions().get(partition).replicas()
        .stream().map(r -> brokerRack(r)).collect(toSet());
}
```

## How to spot

When `racksForPartition` or any similar method always returns an empty collection in tests, check whether the backing data structure is keyed by the same type used at lookup time. A UUID vs name key mismatch, or a stale copy of metadata, will silently produce no-rack results.

## Test

`SubscribedTopicDescriberImplTest` was extended with rack-population assertions that now pass with the `MetadataImage`-backed implementation.
