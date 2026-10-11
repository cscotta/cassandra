**Commit**: 70babd5716 · **Ticket**: KAFKA-18079
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        Properties consumerProps = new Properties(consumerPropsFromFile);
        assertEquals("1000", config.consumerProps().getProperty("request.timeout.ms"));
        assertEquals("group1", config.consumerProps().getProperty("group.id"));

// After
        Properties consumerProps = new Properties();
        consumerProps.putAll(consumerPropsFromFile);
import org.apache.kafka.common.utils.Utils;
        // KafkaShareConsumer uses Utils.propsToMap to convert the properties to a map,
        // so using the same method to check the map has the expected values
        Map<String, Object> configMap = Utils.propsToMap(config.consumerProps());
        assertEquals("1000", configMap.get("request.timeout.ms"));
        assertEquals("group1", configMap.get("group.id"));
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18079 consumer-config does not work with console-share-consumer (#17925).

**Test**
Test modified in ConsoleShareConsumerOptionsTest.java covering the corrected behaviour.
