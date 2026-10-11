**Commit**: be1929c44c · **Ticket**: KAFKA-17680
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
When calling KafkaStreams#close from teardown methods in integration tests, we need to pass timeout to avoid potentially blocking forever during teardown. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            kafkaStreams.close();
            kafkaStreams.close();
            kafkaStreams.close();
            kafkaStreams.close();
            kafkaStreams.close();
        kafkaStreams.close();

// After
import java.time.Duration;
            kafkaStreams.close(Duration.ofSeconds(60));
import java.time.Duration;
            kafkaStreams.close(Duration.ofSeconds(60));
            kafkaStreams.close(Duration.ofSeconds(60));
import java.time.Duration;
            kafkaStreams.close(Duration.ofSeconds(60));
            kafkaStreams.close(Duration.ofSeconds(60));
        kafkaStreams.close(Duration.ofSeconds(60));
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17680 add timeout to streams test teardown (#17346).

**Test**
Test modified in ConsistencyVectorIntegrationTest.java covering the corrected behaviour.
