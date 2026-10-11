**Commit**: bd0ea70912 · **Ticket**: KAFKA-18096
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
            // Join the group if not already part of it, or just send the new subscription to the broker on the next poll.
            requestManagers.consumerHeartbeatRequestManager.get().membershipManager().onSubscriptionUpdated();
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly_KAFKA_17696"))

// After
        // Join the group if not already part of it, or just send the updated subscription
        // to the broker on the next poll. Note that this is done even if no topics matched
        // the regex, to ensure the member joins the group if needed (with empty subscription).
        requestManagers.consumerHeartbeatRequestManager.get().membershipManager().onSubscriptionUpdated();
import static org.mockito.Mockito.clearInvocations;
    @Test
    public void testTopicPatternSubscriptionTriggersJoin() {
        TopicPatternSubscriptionChangeEvent event = new TopicPatternSubscriptionChangeEvent(
            Pattern.compile("topic.*"), Optional.of(new MockRebalanceListener()), 12345);
        setupProcessor(true);
        Cluster cluster = mock(Cluster.class);
        when(metadata.fetch()).thenReturn(cluster);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18096: allow join with regex if no matching topics  (#18024).

**Test**
Test modified in ApplicationEventProcessorTest.java covering the corrected behaviour.
