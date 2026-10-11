**Commit**: 1e95d04639 · **Ticket**: KAFKA-19747
**Subsystem**: clients/src/main/java/org/apache/kafka/common/telemetry/internals/ClientTelemetryReporter.java
**Category**: Wrong constant / default value

**Root cause**
`handleFailedGetTelemetrySubscriptionsRequest` and `handleFailedPushTelemetryRequest` passed `maybeFatalException != null` as the `isFatal` argument to `handleFailedRequest`. This meant any non-null exception — including transient retriable exceptions like `TimeoutException` or `NetworkException` — was treated as fatal, setting the push interval to `Integer.MAX_VALUE` and effectively disabling telemetry permanently. The correct logic is: an exception is fatal only when it is non-null AND not a `RetriableException` (or wrapping one).

**Before/After**
```java
// Before: any non-null exception disables telemetry
public void handleFailedGetTelemetrySubscriptionsRequest(KafkaException maybeFatalException) {
    handleFailedRequest(maybeFatalException != null);
}
public void handleFailedPushTelemetryRequest(KafkaException maybeFatalException) {
    handleFailedRequest(maybeFatalException != null);
}

// After: only non-retriable exceptions are treated as fatal
public void handleFailedGetTelemetrySubscriptionsRequest(KafkaException maybeFatalException) {
    handleFailedRequest(isRetryable(maybeFatalException));
}
private boolean isRetryable(final KafkaException maybeFatalException) {
    return maybeFatalException == null ||
        (maybeFatalException instanceof RetriableException) ||
        (maybeFatalException.getCause() != null && maybeFatalException.getCause() instanceof RetriableException);
}
```

**How to spot**
Look for boolean predicates in error-handling paths that test `exception != null` when the intent is to distinguish fatal vs. transient failures. Any place that converts "exception exists" into "disable permanently" without checking the exception type is a candidate for this bug pattern.

**Test**
Multiple new tests: `testHandleFailedGetTelemetrySubscriptionsRequestWithRetriableException` asserts that after a `TimeoutException` the telemetry interval remains at `DEFAULT_PUSH_INTERVAL_MS` and telemetry is still enabled; `testHandleFailedGetTelemetrySubscriptionsRequestWithFatalException` asserts that after an `AuthorizationException` the interval becomes `Integer.MAX_VALUE` and telemetry is disabled.
