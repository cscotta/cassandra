**Commit**: 4e52a126ee · **Ticket**: KAFKA-17154
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, TaiJuWu <tjwu1217@gmail.com>

**Before/After**
```java
// Before
     * Join the group with the updated subscription, if the member is not part of it yet. If the
     * member is already part of the group, this will only ensure that the updated subscription
        if (state == MemberState.UNSUBSCRIBED) {
            requestManagers.consumerHeartbeatRequestManager.ifPresent(hrm -> hrm.resetPollTimer(event.pollTimeMs()));
            requestManagers.shareHeartbeatRequestManager.ifPresent(hrm -> hrm.resetPollTimer(event.pollTimeMs()));

// After
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
    /**
     * AtomicBoolean to track whether the subscription is updated.
     * If it's true and subscription state is UNSUBSCRIBED, the next {@link #onConsumerPoll()} will change member state to JOINING.
     */
    private final AtomicBoolean subscriptionUpdated = new AtomicBoolean(false);

     * Set {@link #subscriptionUpdated} to true to indicate that the subscription has been updated.
     * The next {@link #onConsumerPoll()} will join the group with the updated subscription, if the member is not part of it yet.
     * If the member is already part of the group, this will only ensure that the updated subscription
        subscriptionUpdated.compareAndSet(false, true);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17154: new consumer subscribe may join group without a call to consumer.poll (#17165).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
