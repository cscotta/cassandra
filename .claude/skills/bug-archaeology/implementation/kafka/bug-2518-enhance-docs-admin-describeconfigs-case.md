**Commit**: 246b165456 · **Ticket**: KAFKA-17359
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- The different behavior of nonexistent resource. For example: nonexistent broker will cause timeout; nonexistent topic will produce UnknownTopicOrPartitionException; nonexistent group will return static/default configs; client_metrics will return empty configs
- The resources (topic and broker resource types are currently supported) this description is out-of-date
- Add some junit test

Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * @param resources The resources (topic and broker resource types are currently supported)
     * @param resources The resources (topic and broker resource types are currently supported)

// After
     * @param resources See relevant type {@link ConfigResource.Type}
     * The different behavior of nonexistent resource:
     * <ul>
     *     <li>{@link ConfigResource.Type#BROKER}:
     *     will throw a {@link org.apache.kafka.common.errors.TimeoutException} exception</li>
     *     <li>{@link ConfigResource.Type#TOPIC}:
     *     will throw a {@link org.apache.kafka.common.errors.UnknownTopicOrPartitionException} exception</li>
     *     <li>{@link ConfigResource.Type#GROUP}:
     *     just return default configs even if the target group is nonexistent</li>
     *     <li>{@link ConfigResource.Type#BROKER_LOGGER}:
     *     will throw a {@link org.apache.kafka.common.errors.TimeoutException} exception</li>
     *     <li>{@link ConfigResource.Type#CLIENT_METRICS}: will return empty configs</li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17359 add tests and enhance the docs of `admin#describeconfigs` for the case of nonexistent resource  (#16947).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
