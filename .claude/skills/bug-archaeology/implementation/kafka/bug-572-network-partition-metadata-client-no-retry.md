**Commit**: ed3af72124 · **Ticket**: KAFKA-19928

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/NetworkPartitionMetadataClient.java

**Category**: Race condition/missing synchronization

**Root cause**
`NetworkPartitionMetadataClient` lazily initialised its underlying `NetworkClient` on the first request, so the very first `ListOffsets` request always failed because the connection had not yet been established. There was also no retry or backoff logic: a single transient disconnect or timeout would permanently fail the metadata fetch. The fix introduces an `ExponentialBackoffManager` and a `RetryTimerTask` so that failed requests are retried with increasing delays, and the `PendingRequest` record now carries the backoff state to coordinate retries across reconnection events.

**Before**
```java
// Before: single attempt, no retry
public CompletableFuture<ListOffsetsResult> fetchOffsets(
        TopicPartition tp, long timestamp) {
    sendRequest(tp, timestamp);
    // no retry on failure
}
```

**After**
```java
// After: exponential backoff retry
class PendingRequest {
    final ExponentialBackoffManager backoffManager;
    // retried via RetryTimerTask on disconnect/timeout
}
```

**How to spot**
Search for `NetworkClient` usages where the client is created lazily and where the first call to `poll()` or `send()` is not guarded by a connection-ready check; any such path will silently drop the first request.

**Test**
Unit-test `NetworkPartitionMetadataClient` by simulating a disconnection on the first request and verifying the request is retried with backoff until it succeeds.
