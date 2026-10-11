**Commit**: fd9de50de1 · **Ticket**: KAFKA-18041
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
This PR updates the key for storing the KIP-714 client instance id for the global consumer to follow a more consistent pattern of the other embedded Kafka Streams consumer clients. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
                            && !entry.getKey().endsWith("GlobalStreamThread"))
                    globalStreamThread.getName(),

// After
    <p>
        In this release the <code>ClientInstanceIds</code> instance stores the global consumer<code>Uuid</code> for the
        <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-714%3A+Client+metrics+and+observability#KIP714:Clientmetricsandobservability-Clientidentificationandtheclientinstanceid">KIP-714</a>
        id with a key of global stream-thread name appended with <code>"-global-consumer"</code> where before it was only the global stream-thread name.
    </p>

                            && !entry.getKey().endsWith("GlobalStreamThread-global-consumer"))
                    globalStreamThread.getName() + "-global-consumer",
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18041: update key for storing global consumer instance id for consistency (#17869).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
