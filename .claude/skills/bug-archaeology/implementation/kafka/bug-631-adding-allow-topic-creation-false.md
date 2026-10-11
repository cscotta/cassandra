**Commit**: 188b86fa83 · **Ticket**: KAFKA-19793
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Added auto topic creation false for both restore and global consumers. Reviewers: Nikita Shupletsov <nshupletsov@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
        new String[] {ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ConsumerConfig.GROUP_PROTOCOL_CONFIG};
        ConsumerConfig.GROUP_PROTOCOL_CONFIG, "classic"
        checkIfUnexpectedUserSpecifiedConsumerConfig(clientProvidedProps, NON_CONFIGURABLE_CONSUMER_DEFAULT_CONFIGS);
        checkIfUnexpectedUserSpecifiedConsumerConfig(clientProvidedProps, NON_CONFIGURABLE_CONSUMER_EOS_CONFIGS);
    private void checkIfUnexpectedUserSpecifiedConsumerConfig(final Map<String, Object> clientProvidedProps,
                                                              final String[] nonConfigurableConfigs) {
        // Streams does not allow users to configure certain consumer/producer configurations, for example,
        // enable.auto.commit. In cases where user tries to override such non-configurable
        // consumer/producer configurations, log a warning and remove the user defined value from the Map.
        // Thus, the default values for these consumer/producer configurations that are suitable for
        // disable auto topic creation
        consumerProps.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");

// After
        new String[] {ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ConsumerConfig.GROUP_PROTOCOL_CONFIG, ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG};
        ConsumerConfig.GROUP_PROTOCOL_CONFIG, "classic",
        ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false"
        checkIfUnexpectedUserSpecifiedClientConfig(clientProvidedProps, NON_CONFIGURABLE_CONSUMER_DEFAULT_CONFIGS);
        checkIfUnexpectedUserSpecifiedClientConfig(clientProvidedProps, NON_CONFIGURABLE_CONSUMER_EOS_CONFIGS);
    private void checkIfUnexpectedUserSpecifiedClientConfig(final Map<String, Object> clientProvidedProps,
                                                            final String[] nonConfigurableConfigs) {
        // Streams does not allow users to configure certain client configurations (consumer/producer),
        // for example, enable.auto.commit or transactional.id. In cases where user tries to override
        // such non-configurable client configurations, log a warning and remove the user defined value
        // from the Map. Thus, the default values for these client configurations that are suitable for
        checkIfUnexpectedUserSpecifiedClientConfig(mainConsumerProps, NON_CONFIGURABLE_CONSUMER_DEFAULT_CONFIGS);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19793 adding allow topic creation false for global and restore consumer (#20723).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
