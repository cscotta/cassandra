**Commit**: 6fd973b4a5 · **Ticket**: KAFKA-16331
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
                              "Setting {}=\"{}\"/\"{}\" requires broker version 2.5 or higher.",
                          StreamsConfig.EXACTLY_ONCE_V2, StreamsConfig.EXACTLY_ONCE_BETA);
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        final boolean eosEnabled = processingMode.startsWith("exactly_once");
    @SuppressWarnings("deprecation")
            if (!StreamsConfig.EXACTLY_ONCE.equals(processingGuarantee) &&
                !StreamsConfig.EXACTLY_ONCE_BETA.equals(processingGuarantee) &&
                !StreamsConfig.EXACTLY_ONCE_V2.equals(processingGuarantee)) {
                System.err.println("processingGuarantee must be either " + StreamsConfig.EXACTLY_ONCE + " or " +
                                       StreamsConfig.EXACTLY_ONCE_BETA + " or " + StreamsConfig.EXACTLY_ONCE_V2);
    @SuppressWarnings("deprecation")

// After
                              "Setting {}=\"{}\" requires broker version 2.5 or higher.",
                          StreamsConfig.EXACTLY_ONCE_V2);
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        final boolean eosEnabled = processingMode.equals("exactly_once_v2");
            if (!StreamsConfig.EXACTLY_ONCE_V2.equals(processingGuarantee)) {
                System.err.println("processingGuarantee must be " + StreamsConfig.EXACTLY_ONCE_V2);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16331: remove eosv1 from kafka streams system tests (#17108).

**Test**
Test modified in BrokerCompatibilityTest.java covering the corrected behaviour.
