## Commit

34e426b517 — KAFKA-19271: Add ConsumerWrapper abstraction for KIP-1071 streams group protocol

## Ticket

KAFKA-19271

## Subsystem

Kafka Streams / consumer group protocol

## Category

Missing override of default method

## Root cause

Testing the KIP-1071 streams group protocol required injecting a custom `Consumer` implementation that could intercept and record calls made by the `StreamThread`. Without a wrapper abstraction, tests had to mock the entire `Consumer` interface, including default methods that changed across API versions. Any new default method added to `Consumer` would silently fall back to the default implementation in test doubles, masking regressions. `ConsumerWrapper` delegates all calls to a wrapped real `Consumer` instance, making it the single place that must be updated when the interface gains new methods.

## Before / After

```java
// Before — tests mocked Consumer directly, missing override risk
Consumer<byte[], byte[]> consumer = mock(Consumer.class);

// After — ConsumerWrapper delegates explicitly, forces override awareness
public class ConsumerWrapper<K, V> implements Consumer<K, V> {
    private final Consumer<K, V> delegate;
    @Override public ConsumerRecords<K, V> poll(Duration timeout) {
        return delegate.poll(timeout);
    }
    // ... all methods explicitly delegated
}
```

## How to spot

Search for test doubles that implement large interfaces via `mock()` without a concrete delegating wrapper. Any interface with more than a handful of methods risks having new default methods silently bypass test instrumentation.

## Test

`ConsumerWrapperTest` verifies that each delegated method forwards arguments and return values correctly.
