**Commit**: f60382bf21 · **Ticket**: KAFKA-18127
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     *             when the broker doesn't support this feature
            handleFatalFailure(exception);
                message = "The cluster doesn't yet support the new consumer group protocol." +
                        " Set group.protocol=classic to revert to the classic protocol until the cluster is upgraded.";
                handleFatalFailure(error.exceptio

// After
     *             when the broker doesn't support this feature. Also, if the consumer attempts to subscribe to a
     *             SubscriptionPattern via {@link #subscribe(SubscriptionPattern)} or
     *             {@link #subscribe(SubscriptionPattern, ConsumerRebalanceListener)} and the broker doesn't
     *             support this feature.
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.protocol.ApiKeys;
    public static final String CONSUMER_PROTOCOL_NOT_SUPPORTED_MSG = "The cluster does not support the new CONSUMER " +
        "group protocol. Set group.protocol=classic on the consumer configs to revert to the CLASSIC protocol " +
        "until the cluster is upgraded.";

            if (isHBApiUnsupportedErrorMsg(exception)) {
                // This is expected to be the case where building the request fails because the node does not support
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18127 validate subscriptionpattern used on v0 hb (#17989).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
