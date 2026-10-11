**Commit**: 0fd8327920 · **Ticket**: KAFKA-20315
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This PR adds unit test coverage for the single in-flight poll event
behavior in AsyncKafkaConsumer and ShareConsumer. Both consumers are expected to submit at most one poll event
(AsyncPollEvent or SharePollEvent) while the previous event is still
in-flight. Repeated poll() iterations (e.g., due to empty fetches) must
not enqueue additional events until the current one completes. Reviewers: Sanskar Jhajharia <sjhajharia@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
    /**
     * Verifies that at most one {@link AsyncPollEvent} is in-flight at a time. When {@code poll()} runs
     * multiple loop iterations (e.g. empty fetches), it must not enqueue a new event while the previous
     * one is still in-flight. This prevents unnecessary queueing. See KAFKA-20315.
     */
    @Test
    public void testPollDoesNotAddNewAsyncPollEventWhenOneIsAlreadyInFlight() {
        // Build a consumer with a mocked FetchBuffer so we can deterministically advance MockTime and avoid
        // tight spinning while poll() waits.
        FetchBuffer fetchBuffer = mock(FetchBuffer.class);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20315: add tests for single in-flight poll events in async and share consumers (#21800).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
