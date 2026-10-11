**Commit**: 284a3db28a · **Ticket**: KAFKA-15908
**Subsystem**: clients/consumer (Consumer interface)
**Category**: State not cleaned up

**Root cause**
`Consumer.poll(long timeout)` was the original millisecond-based poll API, deprecated in favour of `Consumer.poll(Duration)`. It was kept through several releases for backward compatibility but the `includeMetadataInTimeout` branching it introduced into `ClassicKafkaConsumer.poll(Timer, boolean)` added conditional logic to the hot path. Removing it simplifies the internal `poll(Timer, boolean)` to a single code path, removes a source of subtle timing bugs (millisecond truncation vs. Duration), and cleans up the public `Consumer` interface.

**Before/After**
```java
// Before
@Deprecated
ConsumerRecords<K, V> poll(long timeoutMs);

// Inside ClassicKafkaConsumer:
ConsumerRecords<K, V> poll(Timer timer, boolean includeMetadataInTimeout) {
    if (includeMetadataInTimeout) { ... } else { ... }
}

// After – deprecated method removed; private method simplified
ConsumerRecords<K, V> poll(Timer timer) { ... }
```

**How to spot**
When a deprecated API method adds an internal boolean parameter to the hot poll path, the technical debt compounds on every release; once the deprecated method is removed, simplify all internal callers that still carry the vestigial boolean flag.

**Test**
`KafkaConsumerTest`, `ClassicKafkaConsumerTest`, and `MockConsumerTest` updated.
