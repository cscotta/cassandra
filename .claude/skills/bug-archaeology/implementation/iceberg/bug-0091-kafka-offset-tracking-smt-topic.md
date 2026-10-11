# Bug 0091: Kafka Connect source offset tracked under SMT-rewritten topic instead of original

**Commit**: 3f18cd4f69a2110327161972646da226fbbecc13 · **Ticket**: #15880

## Detection Rule

When a Kafka Connect SinkTask records source offsets using `record.topic()` or `record.kafkaPartition()`, verify that the code uses `record.originalTopic()` and `record.originalKafkaPartition()` instead, because SMTs (Single Message Transforms) may have rewritten those values before the task receives the record.

## Code Smell

A `sourceOffsets.put(new TopicPartition(record.topic(), record.kafkaPartition()), ...)` call in a Kafka Connect SinkTask where SMTs may be configured — the framework's offset management and `context.assignment()` operate on original (pre-transform) topic-partition values.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: kafka-connect/SinkWriter

## Root Cause

The Kafka Connect framework delivers records to SinkTasks after applying all configured SMTs, so `record.topic()` and `record.kafkaPartition()` reflect the transformed values. However, consumer offset management and `context.assignment()` use the original, pre-transform topic-partition. By keying `sourceOffsets` on the SMT-rewritten topic, the connector tracked offsets under a key that was never found in `context.assignment()`, preventing proper offset commits and causing re-delivery of already-processed records.

## Example

```java
// BEFORE
sourceOffsets.put(
    new TopicPartition(record.topic(), record.kafkaPartition()),
    new Offset(record.kafkaOffset() + 1, timestamp));
```

```java
// AFTER
// use the original topic and partition to track offsets, as SMTs may have changed
// record.topic() and record.kafkaPartition()
sourceOffsets.put(
    new TopicPartition(record.originalTopic(), record.originalKafkaPartition()),
    new Offset(record.originalKafkaOffset() + 1, timestamp));
```
