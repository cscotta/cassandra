**Commit**: e1dcd383bc · **Ticket**: KAFKA-17927
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                if (userConfiguredIdempotence) {
                    throw new ConfigException("Must set " + MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION + " to at most 5" +
                        " to use the idempotent producer.");
                }
                log.warn("Idempotence will be disabled because {} is set to {}, which is greater than 5. " +
                    "Please note that in v4.0.0 and onward, this will become an error.", MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, inFlightConnection);
                shouldDisableIdempotence = true;
        config = new ProducerConfig(validProps2);
        assertFalse(
            config.getBoolean(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG),
            "idempotence should be disabled when `max.in.flight.requests.per.connection` is greater than 5 and " +
                "`enable.idempotence` config is unset.");

// After
                throw new ConfigException("To use the idempotent producer, " + MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION +
                                          " must be set to at most 5. Current value is " + inFlightConnection + ".");

        ConfigException configException = assertThrows(ConfigException.class, () -> new ProducerConfig(validProps2));
        assertEquals("To use the idempotent producer, " + ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION +
                     " must be set to at most 5. Current value is 6.", configException.getMessage());
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

    @Test
    void testUpperboundCheckOfEnableIdempotence() {
        String inFlightConnection = "6";
        final Map<String, Object> configs = new HashMap<>();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17927 disallow users to configure `max.in.flight.requests.per.connection` bigger than 5 (#17717).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
