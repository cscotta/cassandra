**Commit**: b065c60f49 · **Ticket**: KAFKA-19734
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
## Summary

As a follow-on to the improvements introduced in

[KIP-1091](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1091%3A+Improved+Kafka+Streams+operator+metrics)
it would be useful to add the application-id as a tag to the
`client-state` metric. This allows Kafka Streams developers and
operators to connect metrics containing a `thread-id` (which embeds the
`application-id`) across separate deployments of Kafka Streams
instances, which are members of the same logical application. Reviewers: Bill Bejeck<bbejeck@apache.org> Matthias
 Sax<mjsax@apache.org>

**Before/After**
```java
// Before
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(metrics, "test", "processId", time);
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(new Metrics(), "clientId", "processId", new MockTime());

// After
            applicationId,
    private final String applicationId;
    public static final String APPLICATION_ID_TAG = "application-id";
                              final String applicationId,
        this.applicationId = applicationId;
        tagMap.put(APPLICATION_ID_TAG, applicationId);
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(metrics, "test", "processId", "applicationId", time);
    private final StreamsMetricsImpl streamsMetrics = new StreamsMetricsImpl(new Metrics(), "clientId", "processId", "applicationId", new MockTime());
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19734 adding application tag to clientstate metric (#20766).

**Test**
Test modified in KStreamSessionWindowAggregateProcessorTest.java covering the corrected behaviour.
