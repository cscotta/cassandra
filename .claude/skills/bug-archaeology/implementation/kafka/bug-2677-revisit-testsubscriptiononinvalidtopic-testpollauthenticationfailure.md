**Commit**: f9f480ac7c · **Ticket**: KAFKA-17163
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            TestUtils.waitForCondition(() -> {
                try {
                    consumer.poll(Duration.ZERO);
                    return false;
                } catch (NoOffsetForPartitionException e) {
                    return true;
                }
            }, "Consumer was not able to update fetch positions on continuous calls with 0 timeout");
    // TODO: this test requires rebalance logic which is not yet implemented in the CONSUMER group protocol.
    //       Once it is implemented, this should use both group protocols.
    @EnumSource(value = GroupProtocol.class, names = "CLASSIC")
    public void testPollAuthenticationFailure(GroupProtocol groupProtocol) {

// After
            assertPollEventuallyThrows(consumer, NoOffsetForPartitionException.class,
                    "Consumer was not able to update fetch positions on continuous calls with 0 timeout");
    @EnumSource(GroupProtocol.class)
    public void testPollAuthenticationFailure(GroupProtocol groupProtocol) throws InterruptedException {

        if (groupProtocol == GroupProtocol.CONSUMER) {
            // New consumer poll(ZERO) needs to wait for the event added by a call to poll, to be processed
            // by the background thread, so it can realize there is authentication fail  and then
            // throw the AuthenticationException
            assertPollEventuallyThrows(consumer, AuthenticationException.class,
                    "he consumer was not able to discover metadata errors during continuous polling.");
        } else {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17163 revisit testsubscriptiononinvalidtopic and testpollauthenticationfailure (#16638).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
