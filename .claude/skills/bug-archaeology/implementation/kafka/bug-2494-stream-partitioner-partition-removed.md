**Commit**: e8b27b6a33 · **Ticket**: KAFKA-16335
**Subsystem**: streams/processor/StreamPartitioner
**Category**: Missing override of default method

**Root cause**
`StreamPartitioner` provided both a deprecated `Integer partition(...)` method and a newer `default Optional<Set<Integer>> partitions(...)` method that delegated to `partition()`. This kept the old API alive indefinitely via the default bridge and caused confusion: callers of `partitions()` silently invoked the deprecated `partition()` through the default if the implementation hadn't been updated. The fix removes `partition()` from the interface, makes `partitions()` abstract, and updates all internal implementations (`DefaultStreamPartitioner`, `WindowedStreamPartitioner`) to return `Optional<Set<Integer>>` directly.

**Before/After**
```java
// Before
@Deprecated Integer partition(String topic, K key, V value, int numPartitions);
default Optional<Set<Integer>> partitions(...) {
    Integer p = partition(topic, key, value, numPartitions);
    return p == null ? Optional.empty() : Optional.of(Collections.singleton(p));
}

// After: deprecated method removed; partitions() is now abstract
Optional<Set<Integer>> partitions(String topic, K key, V value, int numPartitions);
```

**How to spot**
A `default` method that bridges from the new API to the old deprecated one is a compatibility shim, not a permanent design. When the deprecated method is removed in a major release, also delete the default bridge and make the new method abstract to force all implementations to migrate.

**Test**
`KStreamRepartitionIntegrationTest` and `KafkaStreamsTest` updated to use `Optional<Set<Integer>>` lambda syntax.
