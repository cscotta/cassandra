**Commit**: 3a1465e14c · **Ticket**: KAFKA-16792
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, TaiJuWu <tjwu1217@gmail.com>, Kirk True <ktrue@confluent.io>, TengYao Chi <kitingiao@gmail.com>

**Before/After**
```java
// Before
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    public void testFetchStableOffsetThrowInPoll(GroupProtocol groupProtocol) {
        assertThrows(UnsupportedVersionException.class, () -> setupThrowableConsumer(groupProtocol).poll(Duration.ZERO));
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    public void testCurrentLag(GroupProtocol groupProtocol) {
        assertEquals(0, client.inFlightRequestCount());

        assertEquals(2, client.inFlightRequestCount());

// After
    @EnumSource(GroupProtocol.class)
    public void testFetchStableOffsetThrowInPoll(GroupProtocol groupProtocol) throws InterruptedException {
        setupThrowableConsumer(groupProtocol);
        TestUtils.waitForCondition(() -> {
            try {
                // For CONSUMER protocol, the offset request is sent in the background thread,
                // so we need to wait until the offset request is sent.
                consumer.poll(Duration.ZERO);
                return false;
            } catch (UnsupportedVersionException e) {
                return true;
            }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16792: enable consumer unit tests that fail to fetch offsets only for new consumer with poll(0) (#16982).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
