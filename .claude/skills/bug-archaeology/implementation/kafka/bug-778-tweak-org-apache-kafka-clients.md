**Commit**: 32c2383bfa · **Ticket**: KAFKA-19658
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
1. Optimize the `equals()`, `hashCode()`, and `toString()` methods in
`OffsetAndMetadata`. 2. Add UT and IT to these modifications.

**Before/After**
```java
// Before
            consumer.commitSync(Map.of(TP, new OffsetAndMetadata(2L)));
            assertEquals(2, consumer.committed(Set.of(TP)).get(TP).offset());
            var offsetsToCommit = Map.of(TP, new OffsetAndMetadata(5L));
            assertEquals(5, consumer.committed(Set.of(TP)).get(TP).offset());
        if (metadata == null)
            this.metadata = OffsetFetchResponse.NO_METADATA;
        else
            this.metadata = metadata;
                Objects.equals(leaderEpoch, that.leaderEpoch);
        return Objects.hash(offset, metadata, leaderEpoch);
                ", leaderEpoch=" + leaderEpoch +


// After
            consumer.commitSync(Map.of(TP, new OffsetAndMetadata(2L, "metadata")));
            OffsetAndMetadata metadata = consumer.committed(Set.of(TP)).get(TP);
            assertEquals(2, metadata.offset());
            assertEquals("metadata", metadata.metadata());
            var offsetsToCommit = Map.of(TP, new OffsetAndMetadata(5L, null));
            metadata = consumer.committed(Set.of(TP)).get(TP);
            assertEquals(5, metadata.offset());
            // null metadata will be converted to an empty string
            assertEquals("", metadata.metadata());
        this.metadata = Objects.requireNonNullElse(metadata, OffsetFetchResponse.NO_METADATA);
    /**
     * Get the metadata of the previously consumed record.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19658 tweak org.apache.kafka.clients.consumer.offsetandmetadata (#20451).

**Test**
Test modified in PlaintextConsumerTest.java covering the corrected behaviour.
