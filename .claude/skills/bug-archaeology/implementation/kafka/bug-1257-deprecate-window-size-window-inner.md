**Commit**: c5f8ae0424 · **Ticket**: KAFKA-16260
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
The `window.size.ms` and `window.inner.class.serde` are not a true
KafkaStreams config, and are ignored when set from a KStreams
application. Both belong on the client. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>
Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
        consumerProperties.put(StreamsConfig.WINDOW_SIZE_MS_CONFIG, 500L);
        if (keyDeserializer instanceof TimeWindowedDeserializer || keyDeserializer instanceof SessionWindowedDeserializer) {
            consumerProperties.setProperty(StreamsConfig.WINDOWED_INNER_CLASS_SERDE,
        consumerProperties.put(StreamsConfig.WINDOW_SIZE_MS_CONFIG, 500L);
        if (keyDeserializer instanceof TimeWindowedDeserializer || keyDeserializer instanceof SessionWindowedDeserializer) {
            consumerProperties.setProperty(StreamsConfig.WINDOWED_INNER_CLASS_SERDE,
                "--property", "key.deserializer." + StreamsConfig.WINDOWED_INNER_CLASS_SERDE + "=" + Serdes.serdeFrom(innerClass).getClass().getName(),

// After
            <li><b>Stream</b>
                <ul>
                    <li>
                        The <code>window.size.ms</code> and <code>window.inner.serde.class</code> in <code>StreamsConfig</code> are deprecated.
                        Use the corresponding string constants defined in <code>TimeWindowedSerializer</code>, <code>TimeWindowedDeserializer</code>, <code>SessionWindowedSerializer</code> and <code>SessionWindowedDeserializer</code> instead.
                    </li>
                </ul>
            </li>
        consumerProperties.put(TimeWindowedDeserializer.WINDOW_SIZE_MS_CONFIG, 500L);
        if (keyDeserializer instanceof TimeWindowedDeserializer) {
            consumerProperties.setProperty(TimeWindowedDeserializer.WINDOWED_INNER_DESERIALIZER_CLASS,
                Serdes.serdeFrom(innerClass).getClass().getName());
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16260: deprecate window.size.ms and window.inner.class.serde in streamsconfig (#18297).

**Test**
Test modified in KStreamAggregationIntegrationTest.java covering the corrected behaviour.
