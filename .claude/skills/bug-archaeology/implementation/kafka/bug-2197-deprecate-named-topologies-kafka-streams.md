**Commit**: 1de4f27ec0 · **Ticket**: KAFKA-17805
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Deprecate for 4.0. This feature will be phased out, with new functionality generally not supporting named topologies, so users are encouraged to migrate off of named topologies as soon as possible. Though who are using this experimental feature are encouraged to reach out by filing a jira ticket so we can better understand your use case and how to support it going forward. Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
 * {@link org.apache.kafka.streams.KafkaStreams} or {@link KafkaStreamsNamedTopologyWrapper} constructors will
 * determine the defaults, which can then be overridden for specific topologies by passing them in when creating the
    // TODO KAFKA-13336: we can remove this reference once we make the Topology/NamedTopology class into an interface and implement it
                    // TODO KAFKA-13281: when we add support for global stores with named topologies we will
                    //  need to add the global stores to the activeStoresOnHost set


// After
    @SuppressWarnings("deprecation")
    @SuppressWarnings("deprecation")
    @SuppressWarnings("deprecation")
 * {@link org.apache.kafka.streams.KafkaStreams} constructor or the {@link KafkaStreamsNamedTopologyWrapper} constructor (deprecated)
 * will determine the defaults, which can then be overridden for specific topologies by passing them in when creating the

    @SuppressWarnings("deprecation")
    @SuppressWarnings("deprecation")
    @SuppressWarnings("deprecation")
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17805: deprecate named topologies in kafka streams (#17513).

**Test**
Test modified in StreamThreadStateStoreProvider.java covering the corrected behaviour.
