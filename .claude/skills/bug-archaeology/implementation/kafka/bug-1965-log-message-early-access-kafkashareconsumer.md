**Commit**: 5480d54d18 · **Ticket**: KAFKA-17544
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
When a KafkaShareConsumer is constructed in AK 4.0, a log message is written warning about the early access nature of the feature. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.slf4j.Logger;

            LogContext logContext = new LogContext();
            Logger log = logContext.logger(getClass());
            log.warn("Share groups and KafkaShareConsumer are part of the early access of KIP-932 and MUST NOT be used in production.");
            Logger log = logContext.logger(getClass());
            log.warn("Share groups and KafkaShareConsumer are part of the early access of KIP-932 and MUST NOT be used in production.");
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17544: add log message for early access use of kafkashareconsumer (#17940).

**Test**
No test added.
