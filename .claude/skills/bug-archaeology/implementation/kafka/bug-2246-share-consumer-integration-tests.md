**Commit**: 374663c64b · **Ticket**: KAFKA-17287
**Subsystem**: core/test (ShareConsumerTest)
**Category**: Missing override of default method

**Root cause**
`KafkaShareConsumer` had no integration test coverage. Without end-to-end tests verifying subscribe/poll/unsubscribe lifecycles, acknowledgement commit callbacks, lock expiry, wakeup/interrupt behaviour, and multi-consumer sharing semantics, regressions in the share consumer implementation would be invisible until production use. The design gap was the absence of a test class analogous to `PlaintextConsumerTest` for the share consumer protocol.

**Before/After**
```java
// Before – no integration test class for KafkaShareConsumer

// After – 1654-line ShareConsumerTest added covering:
// subscribe/unsubscribe/poll lifecycle
// AcknowledgementCommitCallback success and error paths
// acquisition lock expiry (InvalidRecordStateException)
// headers, wakeup, interrupt, multi-consumer delivery
// explicit vs. implicit acknowledgement
```

**How to spot**
When a new consumer type is introduced, verify that a `*ConsumerTest` integration class exists that exercises at minimum: empty-poll, single-record, multi-record, subscribe/unsubscribe lifecycle, and error acknowledgement paths.

**Test**
New test class `ShareConsumerTest` with 30+ test cases is the entire change.
