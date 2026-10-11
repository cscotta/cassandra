## Commit

f26974b16d — KAFKA-19202: ConfiguredSubtopology sorts TopicInfo without Comparator

## Ticket

KAFKA-19202

## Subsystem

Kafka Streams / topology configuration

## Category

Logic error in condition

## Root cause

`ConfiguredSubtopology` populated `repartitionSourceTopics` and `stateChangelogTopics` as sorted sets via `new TreeSet<>(inputMap.values())`. `TreeSet` requires its elements to implement `Comparable`, but `TopicInfo` does not. At runtime Java falls back to a raw cast to `Comparable`, which throws `ClassCastException` as soon as two elements are compared during insertion. The bug was latent because the set was constructed empty in some code paths; any path that inserted more than one element triggered the exception.

## Before / After

```java
// Before — TreeSet without Comparator, ClassCastException on second insert
this.repartitionSourceTopics = Collections.unmodifiableSortedSet(
    new TreeSet<>(repartitionSourceTopics.values()));

// After — explicit Comparator on the topic name field
this.repartitionSourceTopics = Collections.unmodifiableSortedSet(
    new TreeSet<>(Comparator.comparing(TopicInfo::name)));
this.repartitionSourceTopics.addAll(repartitionSourceTopics.values());
```

## How to spot

Search for `new TreeSet<>(collection)` where the element type is a domain object without `implements Comparable`. Any such construction will compile but throw `ClassCastException` at runtime when the set holds two or more elements.

## Test

`ConfiguredSubtopologyTest` was updated to insert multiple `TopicInfo` values and assert correct sort order.
