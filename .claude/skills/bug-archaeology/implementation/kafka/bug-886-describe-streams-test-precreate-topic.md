**Commit**: c058c134d2 · **Ticket**: KAFKA-19496

**Subsystem**: Kafka Streams / DescribeStreamsGroupTest

**Category**: Flaky test

**Root cause**
`DescribeStreamsGroupTest` started a second Streams instance (`streams2`) that subscribed to `INPUT_TOPIC_2`, but the test did not pre-create `INPUT_TOPIC_2` before starting that instance. Kafka auto-created the topic on first use, but the timing of auto-creation relative to Streams startup caused intermittent failures when the topic was not yet available when the Streams application attempted to subscribe. Pre-creating the topic explicitly before starting `streams2` eliminates the race.

**Before/After**
```java
// Before: streams2 started without INPUT_TOPIC_2 existing
KafkaStreams streams2 = buildStreams2(props);
streams2.start();

// After: topic created before streams2 starts
admin.createTopics(List.of(new NewTopic(INPUT_TOPIC_2, 1, (short)1))).all().get();
KafkaStreams streams2 = buildStreams2(props);
streams2.start();
```

**How to spot**
Integration tests that rely on auto-topic-creation for topics consumed by Streams applications are fragile; auto-creation timing is non-deterministic. Always pre-create all input topics before starting any Streams instance in tests.

**Test**
`DescribeStreamsGroupTest` passes reliably after pre-creating `INPUT_TOPIC_2` before starting the second Streams application.
