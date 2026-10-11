**Commit**: 09be14bb09 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: loicgreffier <loic.greffier@michelin.com>

Minor code improvements across different classed, related to the `ProcessingExceptionHandler` implementation (KIP-1033). Reviewers: Bruno Cadonna <bruno@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        log.warn("Exception caught during message processing, " +
        return this.topic;
        return this.partition;
        return this.offset;
        return this.headers;
        return this.sourceRawKey;
        return this.sourceRawValue;
        return this.processorNodeId;
        return this.taskId;
import org.apache.kafka.common.KafkaException;
public class FailedProcessingException extends KafkaException {
    public FailedProcessingException(final Throwable throwable) {

// After
        log.error("Exception caught during message processing, " +
        return topic;
        return partition;
        return offset;
        return headers;
        return sourceRawKey;
        return sourceRawValue;
        return processorNodeId;
        return taskId;
import org.apache.kafka.streams.errors.StreamsException;
public class FailedProcessingException extends StreamsException {
    public FailedProcessingException(final Exception exception) {
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: fix processing exception handler (#16663).

**Test**
No test added.
