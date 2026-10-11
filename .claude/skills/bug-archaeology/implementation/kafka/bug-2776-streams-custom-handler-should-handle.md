**Commit**: 15a4501bde · **Ticket**: KAFKA-16508
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
For a non-existing output topic, Kafka Streams ends up in an infinite retry loop, because the returned TimeoutException extends RetriableException. This PR updates the error handling pass for this case and instead of retrying calls the ProductionExceptionHandler to allow breaking the infinite retry loop. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            if (exception instanceof RetriableException) {

// After
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
            if (isRetriable(exception)) {
    /**
     * The `TimeoutException` with root cause `UnknownTopicOrPartitionException` is considered as non-retriable
     * (despite `TimeoutException` being a subclass of `RetriableException`, this particular case is explicitly excluded).
    */
    private boolean isRetriable(final Exception exception) {
        return exception instanceof RetriableException &&
                (!(exception instanceof TimeoutException) || exception.getCause() == null
                        || !(exception.getCause() instanceof UnknownTopicOrPartitionException));
    }

```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16508: streams custom handler should handle the timeout exceptions (#16450).

**Test**
Test modified in CustomHandlerIntegrationTest.java covering the corrected behaviour.
