**Commit**: 21e67b3d21 · **Ticket**: KAFKA-16331
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Bill Bejeck <bbejeck@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
@SuppressWarnings("deprecation")
        STREAMS_CONFIG.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        STREAMS_CONFIG.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    @ParameterizedTest
    @ValueSource(strings = {StreamsConfig.EXACTLY_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    public void shouldWorkWithUncleanShutdownWipeOutStateStore(final String eosConfig) throws InterruptedException {
        STREAMS_CONFIG.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, eosConfig);
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE, StreamsConfig.EXACTLY_ONCE_V2})

// After
import org.junit.jupiter.api.Test;
        STREAMS_CONFIG.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        STREAMS_CONFIG.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
    @Test
    public void shouldWorkWithUncleanShutdownWipeOutStateStore() throws InterruptedException {
        STREAMS_CONFIG.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
    @ValueSource(strings = {StreamsConfig.AT_LEAST_ONCE, StreamsConfig.EXACTLY_ONCE_V2})
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16331 remove eosv1 from kafka streams integration tests (#17110).

**Test**
Test modified in EOSUncleanShutdownIntegrationTest.java covering the corrected behaviour.
