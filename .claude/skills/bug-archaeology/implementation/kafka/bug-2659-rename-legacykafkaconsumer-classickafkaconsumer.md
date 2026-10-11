**Commit**: d260b06180 · **Ticket**: KAFKA-17060
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lianetmr@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
 * <em>Note:</em> per its name, this implementation is left for backward compatibility purposes. The updated consumer
 * group protocol (from KIP-848) introduces allows users continue using the legacy "classic" group protocol.
 * This class should not be invoked directly; users should instead create a {@link KafkaConsumer} as before.
public class LegacyKafkaConsumer<K, V> implements ConsumerDelegate<K, V> {
    // currentThread holds the threadId of the current thread accessing LegacyKafkaConsumer
    LegacyKafkaConsumer(ConsumerConfig config, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
    LegacyKafkaConsumer(LogContext logContext,
                        Time time,
                        ConsumerConfig config,
                        Deserializer<K> keyDeserializer,
                        Deserializer<V> valueDeserializer,
                        KafkaClient client,

// After
 * This {@link ConsumerDelegate} implementation exists for backward compatibility to allow users to continue to use
 * the classic group protocol (pre-KIP 848).
 *
 * <p/>
 *
 * <em>Note:</em> This class should not be invoked directly; users should instead create and use the
 * {@link KafkaConsumer} API as before.
public class ClassicKafkaConsumer<K, V> implements ConsumerDelegate<K, V> {
    // currentThread holds the threadId of the current thread accessing this Consumer
    ClassicKafkaConsumer(ConsumerConfig config, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
    ClassicKafkaConsumer(LogContext logContext,
                         Time time,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17060 rename legacykafkaconsumer to classickafkaconsumer (#16683).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
