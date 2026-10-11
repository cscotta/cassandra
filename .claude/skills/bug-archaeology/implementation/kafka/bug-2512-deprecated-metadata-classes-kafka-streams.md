**Commit**: 8ceb05d203 · **Ticket**: KAFKA-16329
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import java.util.stream.Collectors;
    /**
     * Find all currently running {@code KafkaStreams} instances (potentially remotely) that use the same
     * {@link StreamsConfig#APPLICATION_ID_CONFIG application ID} as this instance (i.e., all instances that belong to
     * the same Kafka Streams application) and return {@link StreamsMetadata} for each discovered instance.
     * <p>
     * Note: this is a point in time view and it may change due to partition reassignment.
     *
     * @return {@link StreamsMetadata} for each {@code KafkaStreams} instances of this application
     * @deprecated since 3.0.0 use {@link KafkaStreams#metadataForAllStreamsClients}
     */
    @Deprecated

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16329: remove deprecated iq metadata classes in kafka streams (#16762).

**Test**
No test added.
