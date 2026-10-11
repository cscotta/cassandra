**Commit**: 1e16e16f64 · **Ticket**: KAFKA-16730
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This is the initial version of the share group consumer client code. It implements the complete ShareConsumer interface. There are unit tests, but not integration tests yet since those would depend upon complete broker code, which is not available at this point. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>,  Lianet Magrans <lianetmr@gmail.com>

**Before/After**
```java
// Before
        CONNECT;
        if (protocolType == ProtocolType.CONSUMER) {
 *     <em>This is an early access feature introduced by KIP-932. It is not suitable for production use until it is
 *     fully implemented and released.</em>
 * This client can communicate with brokers that are version 4.0.0 or newer. You will receive an

// After
        CONNECT,
        SHARE;
        if ((protocolType == ProtocolType.CONSUMER) || (protocolType == ProtocolType.SHARE)) {
import org.apache.kafka.clients.consumer.internals.metrics.KafkaShareConsumerMetrics;
 *     <em>This is an early access feature under development which is introduced by KIP-932.
 *     It is not suitable for production use until it is fully implemented and released.</em>
 * This client can communicate with brokers that are a version that supports share groups. You will receive an

    KafkaShareConsumerMetrics kafkaShareConsumerMetrics() {
        return delegate.kafkaShareConsumerMetrics();
    }
/*
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16730: initial version of share group consumer client code (#16461).

**Test**
Test modified in KafkaShareConsumerMetricsTest.java covering the corrected behaviour.
