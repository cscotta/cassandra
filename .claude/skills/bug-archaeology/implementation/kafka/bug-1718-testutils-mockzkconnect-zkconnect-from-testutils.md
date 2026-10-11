**Commit**: d874aa42f3 · **Ticket**: KAFKA-18368
**Subsystem**: jmh-benchmarks/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            null,
    TestUtils.createBrokerConfigs(2, null).zipWithIndex.map{ case (props, index) =>
    val tsDisabledProps = TestUtils.createBrokerConfigs(1, null).head
    val tsDisabledProps = TestUtils.createBrokerConfigs(1, null).head
    val configs = TestUtils.createBrokerConfigs(brokerCount, null)
      null,
    FixedPortTestUtils.createBrokerConfigs(brokerCount, null, enableControlledShutdown = false)

// After
    TestUtils.createBrokerConfigs(2).zipWithIndex.map{ case (props, index) =>
    val tsDisabledProps = TestUtils.createBrokerConfigs(1).head
    val tsDisabledProps = TestUtils.createBrokerConfigs(1).head
    val configs = TestUtils.createBrokerConfigs(brokerCount)
    FixedPortTestUtils.createBrokerConfigs(brokerCount, enableControlledShutdown = false)
```

**How to spot**
When reviewing jmh-benchmarks/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18368 remove testutils#mockzkconnect and remove zkconnect from testutils#createbrokerconfig (#18352).

**Test**
Test modified in DescribeTopicPartitionsRequestHandlerTest.java covering the corrected behaviour.
