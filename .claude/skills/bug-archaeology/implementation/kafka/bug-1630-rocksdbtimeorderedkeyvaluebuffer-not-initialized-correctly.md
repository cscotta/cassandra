**Commit**: 30f94b5320 · **Ticket**: KAFKA-18479
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
RocksDBTimeOrderedKeyValueBuffer is not initialize with serdes provides
via Joined, but always uses serdes from StreamsConfig. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
        streamsConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass());
        streamsConfig.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        final KeyValueIterator<Long, ValueAndTimestamp<String>> all = store.all();
        final KeyValue<Long, ValueAndTimestamp<String>> onlyEntry = all.next();
        try {
        } finally {
            all.close();
        final KStream<Long, String> leftStream = builder.stream(INPUT_TOPIC_LEFT);
        final KTable<Long, String> rightTable = builder.table(INPUT_TOPIC_RIGHT, Materialized.as(
        final Properties streamsConfig = setupConfigsAndUtils(cacheEnabled);
        leftStream.join(rightTable, valueJoiner, JOINED).to(OUTPUT_TOPIC);

// After
        return setupConfigsAndUtils(cacheEnabled, true);
    }

    Properties setupConfigsAndUtils(final boolean cacheEnabled, final boolean setSerdes) {
        if (setSerdes) {
            streamsConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.LongSerde.class);
            streamsConfig.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        }
        try (final KeyValueIterator<Long, ValueAndTimestamp<String>> all = store.all()) {
            final KeyValue<Long, ValueAndTimestamp<String>> onlyEntry = all.next();
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18479: rocksdbtimeorderedkeyvaluebuffer not initialized correctly (#18490).

**Test**
Test modified in AbstractJoinIntegrationTest.java covering the corrected behaviour.
