**Commit**: 6acc220e03 · **Ticket**: KAFKA-15773
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public void testGroupRemoteAssignorUnusedInGenericProtocol() {
        final ConsumerConfig config = new ConsumerConfig(props);
        consumer = newConsumer(config);

        assertTrue(config.unused().contains(ConsumerConfig.GROUP_REMOTE_ASSIGNOR_CONFIG));
            try (AutoCloseable protocolConsumerGroupExecutor = consumerGroupClosable(groupProtocol, group, topic, Collecti

// After
        checkGroupRemoteAssignor();
    private void checkGroupRemoteAssignor() {
        if (getString(GROUP_PROTOCOL_CONFIG).equalsIgnoreCase(GroupProtocol.CLASSIC.name()) && getString(GROUP_REMOTE_ASSIGNOR_CONFIG) != null && !getString(GROUP_REMOTE_ASSIGNOR_CONFIG).isEmpty()) {
            throw new ConfigException(GROUP_REMOTE_ASSIGNOR_CONFIG + " cannot be set when " + GROUP_PROTOCOL_CONFIG + "=" + GroupProtocol.CLASSIC.name());
        }
    }

    @Test
    public void testRemoteAssignorWithClassicGroupProtocol() {
        String remoteAssignorName = "SomeAssignor";
        final Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15773 group protocol configuration should be validated (#16543).

**Test**
Test modified in ConsumerConfigTest.java covering the corrected behaviour.
