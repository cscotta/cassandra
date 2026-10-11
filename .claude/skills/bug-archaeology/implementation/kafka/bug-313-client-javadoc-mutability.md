## Commit
05185c1ef8

## Ticket
KAFKA-20341

## Subsystem
Clients / Javadoc

## Category
Documentation / clarification

## Root cause
Javadoc for `KafkaConsumer.assignment()`, `subscription()`, and `metrics()` was ambiguous about mutability and liveness. Callers were unsure whether the returned collection was a snapshot or a live view, and whether it was modifiable. Incorrect assumptions could lead to `ConcurrentModificationException` or stale data.

## Before
```java
/** @return The set of partitions currently assigned to this consumer */
public Set<TopicPartition> assignment() { ... }
```

## After
```java
/**
 * @return An immutable snapshot of the set of partitions currently assigned to this consumer.
 *         The snapshot is taken at the time of the call and will not reflect subsequent changes.
 */
public Set<TopicPartition> assignment() { ... }
```

## How to spot
Any public method returning a collection without documenting whether it is a snapshot vs. live view and whether it is modifiable. Pay special attention to `metrics()` (live unmodifiable view) vs. `assignment()`/`subscription()` (immutable snapshots).

## Test
Documentation change only; no behavioral change.
