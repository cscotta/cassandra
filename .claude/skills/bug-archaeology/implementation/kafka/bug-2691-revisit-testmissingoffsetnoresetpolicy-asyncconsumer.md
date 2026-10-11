**Commit**: f09ead1483 · **Ticket**: KAFKA-17132
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    // TODO: this test triggers a bug with the CONSUMER group protocol implementation.
    //       The bug will be investigated and fixed so this test can use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    public void testMissingOffsetNoResetPolicy(GroupProtocol groupProtocol) {
        assertThrows(NoOffsetForPartitionException.class, () -> consumer.poll(Duration.ZERO));

// After
    @EnumSource(value = GroupProtocol.class)
    public void testMissingOffsetNoResetPolicy(GroupProtocol groupProtocol) throws InterruptedException {

        if (groupProtocol == GroupProtocol.CONSUMER) {
            // New consumer poll(ZERO) needs to wait for the offset fetch event added by a call to poll, to be processed
            // by the background thread, so it can realize there are no committed offsets and then
            // throw the NoOffsetForPartitionException
            TestUtils.waitForCondition(() -> {
                try {
                    consumer.poll(Duration.ZERO);
                    return false;
                } catch (NoOffsetForPartitionException e) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17132 revisit testmissingoffsetnoresetpolicy for asyncconsumer (#16587).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
