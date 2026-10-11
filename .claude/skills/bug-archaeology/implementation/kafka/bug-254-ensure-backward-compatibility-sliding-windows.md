**Commit**: 7b8549f3c4 · **Ticket**: KAFKA-20194
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
By default, the DSL should expose it's state-stores as ts-stores, as
long as header format is not enabled; otherwise, it would be a backward
incompatible change. This PR ensures that the builders are creating the correct state stores,
depending on the format, and we insert an adaptor to allow the DSL
Processors to work only against headers-store interface. Reviewers: Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.kstream.SlidingWindows;
    @Test
    public void processorShouldAccessKStreamSlidingReducedKTableStoreAsTimestampedStore() {
        verifyWindow(builder
            .stream("input-topic", Consumed.with(Serdes.String(), Serdes.String()))
            .groupByKey()
            .windowedBy(SlidingWindows.ofTimeDifferenceWithNoGrace(Duration.ofHours(1L)))
            .reduce(
                (value, aggregate) -> value,
                Materialized.<String, String, WindowStore<Bytes, byte[]>>as("table-store").withKeySerde(Serdes.String()).withValueSerde(Serdes.String())
            )
        );
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20194: ensure backward compatibility for sliding windows (#21949).

**Test**
Test modified in PapiDslIntegrationTest.java covering the corrected behaviour.
