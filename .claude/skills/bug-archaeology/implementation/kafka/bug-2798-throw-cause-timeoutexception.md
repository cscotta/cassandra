**Commit**: f4cbf71ea6 · **Ticket**: KAFKA-16965
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Add the cause of TimeoutException for Producer send() errors. Reviewers: Matthias J. Sax <matthias@confluent.io>,  Lianet Magrans <lianetmr@gmail.com>, Artem Livshits <alivshits@confluent.io>, Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
                throw new TimeoutException(
                        String.format("Topic %s not present in metadata after %d ms.",
                                topic, maxWaitMs));
                throw new TimeoutException(partitionsCount == null ?
                                partition, topic, partitionsCount, maxWaitMs));

// After
import org.apache.kafka.common.errors.RetriableException;
                final String errorMessage = String.format("Topic %s not present in metadata after %d ms.",
                        topic, maxWaitMs);
                if (metadata.getError(topic) != null) {
                    throw new TimeoutException(errorMessage, metadata.getError(topic).exception());
                }
                throw new TimeoutException(errorMessage);
                final String errorMessage = partitionsCount == null ?
                                partition, topic, partitionsCount, maxWaitMs);
                if (metadata.getError(topic) != null && metadata.getError(topic).exception() instanceof RetriableException) {
                    throw new TimeoutException(errorMessage, metadata.getError(topic).exception());
                }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16965: throw cause of timeoutexception (#16344).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
