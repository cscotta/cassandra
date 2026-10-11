**Commit**: 9e424755d4 · **Ticket**: KAFKA-17439
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.consumer.internals.events.CreateFetchRequestsEvent;
                    // before returning the fetched records, we can send off the next round of fetches
                    // and avoid block waiting for their responses to enable pipelining while the user
                    // is handling the fetched records.
                    //
                    // NOTE: since the consumed position has already been updated, we must not allow
                    // wakeups or any other errors to be triggered prior to returning the fetched records.
                    sendPrefetches(timer);

        // send any new fetches (won't resend pending fetches)
        sendFetches(timer);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17439: make polling for new records an explicit action/event in the new consumer (#17035).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
