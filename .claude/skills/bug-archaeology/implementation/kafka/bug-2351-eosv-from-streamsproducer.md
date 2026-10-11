**Commit**: 9685aa7547 · **Ticket**: KAFKA-16331
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.stream.Collectors;
import static org.apache.kafka.streams.processor.internals.ClientUtils.taskProducerClientId;
                null,
                taskId,
    Set<String> producerClientIds() {
        if (threadProducer != null) {
            return Collections.singleton(threadProducerClientId(threadId));
        } else {
            return taskProducers.keySet()
                                .stream()
                                .map(taskId -> taskProducerClientId(threadId, taskId))
                                .collect(Collectors.toSet());

// After
    String producerClientIds() {
        return threadProducerClientId(threadId);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16331 remove eosv1 from streamsproducer (#17212).

**Test**
Test modified in ActiveTaskCreatorTest.java covering the corrected behaviour.
