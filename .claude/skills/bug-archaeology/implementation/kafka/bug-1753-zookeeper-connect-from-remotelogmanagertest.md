**Commit**: bc7a1a8969 · **Ticket**: KAFKA-18317
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        Properties props = kafka.utils.TestUtils.createDummyBrokerConfig();
        props.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());
        props.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());
        props.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());
        defaultProps.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());
        customProps.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());
        defaultProps.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());
        customProps.put("zookeeper.connect", kafka.utils.TestUtils.MockZkConnect());

// After
    private final Properties brokerConfig = kafka.utils.TestUtils.createDummyBrokerConfig();
        Properties props = brokerConfig;
        props.putAll(brokerConfig);
        props.putAll(brokerConfig);
        props.putAll(brokerConfig);
        defaultProps.putAll(brokerConfig);
        customProps.putAll(brokerConfig);
        defaultProps.putAll(brokerConfig);
        customProps.putAll(brokerConfig);
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18317 remove zookeeper.connect from remotelogmanagertest (#18283).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
