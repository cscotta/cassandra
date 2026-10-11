**Commit**: 2f9b236259 · **Ticket**: KAFKA-17294
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
The original behavior was implemented to maintain the behavior of the Classic consumer, where the ConsumerCoordinator would do the same when handling the OffsetFetchResponse. This behavior is being updated for the legacy coordinator as part of KAFKA-17279, to retry on all retriable errors. We should review and update the CommitRequestManager to align with this, and retry on all retriable errors, which seems sensible when fetching offsets. The corresponding PR for classic consumer is #16826

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                future.completeExceptionally(responseError.exception());
                future.completeExceptionally(responseError.exception());
                future.completeExceptionally(responseError.exception());
                future.completeExceptionally(responseError.exception());
                // Fail with a non-retriable KafkaException for all unexpected errors (even if
                // they are retriable)
            failAndRemoveExpiredFetchRequests();

        /**
         * Find the unsent fetch requests that have expired, remove them and complete their
         * futures with a TimeoutException.
         */

// After
import org.apache.kafka.common.errors.ApiException;
            ApiException exception = responseError.exception();
                future.completeExceptionally(exception);
                future.completeExceptionally(exception);
                future.completeExceptionally(exception);
                future.completeExceptionally(exception);
            } else if (exception instanceof RetriableException) {
                future.completeExceptionally(exception);
                // Fail with a non-retriable KafkaException for all unexpected errors
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17294 handle retriable errors when fetching offsets in new consumer (#16833).

**Test**
Test modified in CommitRequestManagerTest.java covering the corrected behaviour.
